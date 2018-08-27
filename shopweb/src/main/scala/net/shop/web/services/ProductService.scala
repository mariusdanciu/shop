package net.shop
package web.services

import net.shift.common.TimeUtils.duration
import net.shift.common._
import net.shift.engine.Attempt
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.http.{BinaryPart, MultiPartBody}
import net.shift.io.{FileSystem, IO, LocalFileSystem}
import net.shift.loc.{Language, Loc}
import net.shift.security.Permission
import net.shift.server.http.Request
import net.shift.server.http.Responses._
import net.shop.model.{FieldError, ShopError, _}
import net.shop.utils.ShopUtils
import net.shop.utils.ShopUtils.{dataPath, _}

import scala.util.{Failure, Success, Try}


trait ProductService extends TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService
  with ServiceDependencies {

  def deleteProduct(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- delete
    Path(_, _ :: "product" :: id :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
  } yield {
    store.deleteProduct(id) match {
      case Success(num) =>
        fs.deletePath(Path(s"${dataPath}/products/$id"))
        service(_ (ok.withJsonBody("{\"href\": \"/\"}")))
      case Failure(ShopError(msg, _)) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case Failure(t) => service(_ (notFound))
    }
  }

  def updateProduct(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- post
    Path(_, _ :: "product" :: "update" :: pid :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp, false) match {
      case (files, params, Valid(o)) =>
        val cpy = o.copy(name = normalizeName(o.title_?(r.language.name)))

        val delImages = params.get("delimg") match {
          case Some(list) => list
          case _ => Nil
        }

        (for {
          p <- store.productById(pid)
          u <- store.updateProduct(cpy)
        } yield {
          files.map { f =>
            writeImages(u, f._1, f._2)
          }

          delImages.map { img =>
            fs.deletePath(Path(s"$dataPath/products/$u/thumb/$img"))
            fs.deletePath(Path(s"$dataPath/products/$u/normal/$img"))
            fs.deletePath(Path(s"$dataPath/products/$u/large/$img"))
          }

          service(_ (ok.withJsonBody("{\"href\": \"" + ShopUtils.productPage(cpy) + "\"}")))
        }) match {
          case Success(s) => s
          case Failure(ShopError(msg, _)) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case Failure(t) =>
            log.error("Error", t)
            service(_ (serverError))
        }

      case (_, _, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)
    }
  }

  private def writeImages(id: String, fileName: String, content: Array[Byte]) = {
    ImageUtils.resizeImage(104, content, s"${dataPath}/products/$id/thumb/$fileName")
    ImageUtils.resizeImage(290, content, s"${dataPath}/products/$id/normal/$fileName")
    ImageUtils.resizeImage(1000, content, s"${dataPath}/products/$id/large/$fileName")
  }

  def createProduct(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- post
    Path(_, _ :: "product" :: "create" :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
    mp <- multipartForm
  } yield {
    val extracted = duration(extract(r.language, None, "create_", mp, true)) { d =>
      log.debug("Extraction : " + d)
    }

    extracted match {
      case (files, params, Valid(o)) =>
        try {

          val cpy = o.copy(
            name = normalizeName(o.title_?(r.language.name)))


          val create: Try[String] = duration(store.createProduct(cpy)) { d =>
            log.debug("Persist : " + d)
          }

          create match {
            case Success(p) =>
              duration(
                files.map { f => writeImages(p, f._1, f._2) }) { d => log.debug("Write files: " + d) }
              service(_ (created.withJsonBody("{\"href\": \"" + ShopUtils.productPage(cpy) + "\"}")))
            case Failure(ShopError(msg, _)) =>
              service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
            case Failure(t) =>
              error("Cannot create product ", t)
              service(_ (serverError))
          }
        } catch {
          case t: Throwable => t.printStackTrace()
            error("Cannot create product ", t)
            service(_ (serverError))
        }

      case (_, _, Invalid(msgs)) =>
        log.debug("Send FAIL")
        implicit val l = r.language
        validationFail(msgs)

    }
  }

  private def extract(implicit loc: Language,
                      id: Option[String],
                      fieldPrefix: String,
                      multipart: MultiPartBody,
                      checkExists: Boolean
                     ): (List[(String, Array[Byte])], Map[String, List[String]], Validation[ProductDetail, FieldError]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _ => false
    }

    def stockFunc(in: String) = if (in.isEmpty()) None else Option(in.toInt)

    val params = extractParams(text)
    val files = extractProductFiles(bins)

    val product = ((ProductDetail.apply _).curried) (id getOrElse store.makeID)
    val ? = Loc.loc0(loc) _

    val productFormlet = Validator(product) <*>
      Validator(validateDefault("")) <*>
      Validator {
        val v = validateMapField(fieldPrefix + "title", ?("title").text)
        if (checkExists)
          checkNameExists(v, fieldPrefix + "title", store)
        else v
      } <*>
      Validator(validateMapField(fieldPrefix + "description", ?("description").text)) <*>
      Validator(validateSpecs(fieldPrefix)) <*>
      Validator(validateDouble(fieldPrefix + "price", ?("price").text)) <*>
      Validator(validateOptional(fieldPrefix + "discount_price", s => Option(s.toDouble))) <*>
      Validator(validateDefault(0)) <*>
      Validator(validateOptional(fieldPrefix + "position", s => Option(s.toInt))) <*>
      Validator(validateOptional(fieldPrefix + "presentation_position", s => Option(s.toInt))) <*>
      Validator(validateBoolean(fieldPrefix + "unique")) <*>
      Validator(validateOptional(fieldPrefix + "stock", stockFunc)) <*>
      Validator(validateListField(fieldPrefix + "categories", ?("categories").text)) <*>
      Validator(optionalListField(fieldPrefix + "keywords"))

    try {
      (files, params, productFormlet validate params flatMap {
        case p@ProductDetail(_,
        _,
        _,
        _,
        _,
        price,
        Some(discountPrice),
        _,
        _,
        _,
        _,
        _,
        _,
        _) if (discountPrice >= price) => Invalid(List(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text)))
        case p => Valid(p)
      })
    } catch {
      case e: Exception =>
        (Nil, params, Invalid(List(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text))))
    }
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

}
