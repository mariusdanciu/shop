package net.shop
package web.services

import net.shift.common.TimeUtils.duration
import net.shift.common._
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.http.{BinaryPart, MultiPartBody}
import net.shift.io.{FileSystem, IO, LocalFileSystem}
import net.shift.loc.{Language, Loc}
import net.shift.security.Permission
import net.shift.server.http.Responses._
import net.shop.api.{ProductDetail, ShopError}
import net.shop.model.FieldError
import net.shop.utils.ShopUtils
import net.shop.utils.ShopUtils.dataPath
import net.shop.utils.ShopUtils._


trait ProductService extends TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService
  with ServiceDependencies {

  def deleteProduct(implicit fs: FileSystem) = for {
    r <- delete
    Path(_, _ :: "product" :: id :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
  } yield {
    store.deleteProducts(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"${dataPath}/products/$id"))
        service(_ (ok.withJsonBody("{\"href\": \"/\"}")))
      case scala.util.Failure(ShopError(msg, _)) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t) => service(_ (notFound))
    }
  }

  def updateProduct(implicit fs: FileSystem) = for {
    r <- post
    Path(_, _ :: "product" :: "update" :: pid :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp, false) match {
      case (files, Valid(o)) =>
        val cpy = o.copy(
          name = normalizeName(o.title_?(r.language.name)),
          images = Set(files.map(f => f._2): _*).toList)

        (for {
          p <- store.productById(pid)
          u <- store.updateProducts(cpy.copy(images = p.images ++ cpy.images))
        } yield {
          files.map { f =>
            IO.arrayProducer(f._3)(LocalFileSystem.writer(Path(s"${dataPath}/products/${u.head}/${f._1}")))
          }
          service(_ (ok.withJsonBody("{\"href\": \"" + ShopUtils.productPage(pid) + "\"}")))
        }) match {
          case scala.util.Success(s) => s
          case scala.util.Failure(ShopError(msg, _)) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) => service(_ (serverError))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)
    }
  }

  def createProduct(implicit fs: FileSystem) = for {
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
      case (files, Valid(o)) =>
        val cpy = o.copy(
          name = normalizeName(o.title_?(r.language.name)),
          images = Set(files.map(f => f._2): _*).toList)
        val create = duration(store.createProducts(cpy)) { d =>
          log.debug("Persist : " + d)
        }

        create match {
          case scala.util.Success(p) =>
            duration(
              files.map { f =>
                IO.arrayProducer(f._3)(LocalFileSystem.writer(Path(s"${dataPath}/products/${p.head}/${f._1}")))
              }) { d => log.debug("Write files: " + d) }
            service(_ (created.withJsonBody("{\"href\": \"" + ShopUtils.productPage(cpy) + "\"}")))
          case scala.util.Failure(ShopError(msg, _)) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            service(_ (serverError))
        }

      case (_, Invalid(msgs)) =>
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
                     ): (List[(String, String, Array[Byte])], Validation[ProductDetail, FieldError]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _ => false
    }

    def stockFunc(in: String) = if (in.isEmpty()) None else Option(in.toInt)

    val params = extractParams(text)
    val files = extractProductBins(bins)

    val product = ((ProductDetail.apply _).curried) (id)
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
      Validator(validateDefault(Nil)) <*>
      Validator(optionalListField(fieldPrefix + "keywords"))

    try {
      (files, productFormlet validate params flatMap {
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
        _,
        _) if (discountPrice >= price) => Invalid(List(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text)))
        case p => Valid(p)
      })
    } catch {
      case e: Exception =>
        e.printStackTrace()
        (Nil, Invalid(List(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text))))
    }
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

}
