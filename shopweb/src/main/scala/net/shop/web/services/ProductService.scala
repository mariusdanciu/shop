package net.shop
package web.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import net.shift.common.Config
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.TimeUtils.duration
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.MultiPartBody
import net.shift.io.LocalFileSystem
import net.shift.io.FileSystem
import net.shift.io.IO
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.api.ProductDetail
import net.shop.api.ShopError
import net.shop.model.FieldError
import net.shop.model.ValidationFail
import net.shop.web.ShopApplication
import utils.ShopUtils.dataPath
import net.shift.common.Valid
import net.shift.common.Validation
import net.shift.common.Invalid
import net.shift.common.Validator
import net.shift.engine.http.HttpPredicates._
import net.shift.http.Responses._
import net.shift.http.ContentType._
import net.shift.security.Permission

trait ProductService extends TraversingSpec
    with DefaultLog
    with FormValidation
    with SecuredService
    with ServiceDependencies {

  def deleteProduct(implicit fs: FileSystem) = for {
    r <- delete
    Path(_, _ :: "product" :: "delete" :: id :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
  } yield {
    store.deleteProducts(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"${dataPath}/products/$id"))
        service(_(ok))
      case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(notFound))
    }
  }

  def updateProduct(implicit fs: FileSystem) = for {
    r <- post
    Path(_, _ :: "product" :: "update" :: pid :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    _ <- permissions("Unauthorized", Permission("write"))
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp) match {
      case (files, Valid(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)

        (for {
          p <- store.productById(pid)
          u <- store.updateProducts(cpy.copy(images = p.images ++ cpy.images))
        } yield {
          files.map { f =>
            IO.arrayProducer(f._3)(LocalFileSystem.writer(Path(s"${dataPath}/products/${u.head}/${f._1}")))
          }
          service(_(created))
        }) match {
          case scala.util.Success(s)                 => s
          case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t)                 => service(_(serverError))
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
    val extracted = duration(extract(r.language, None, "create_", mp)) { d =>
      log.debug("Extraction : " + d)
    }

    extracted match {
      case (files, Valid(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)
        val create = duration(store.createProducts(cpy)) { d =>
          log.debug("Persist : " + d)
        }

        create match {
          case scala.util.Success(p) =>
            duration(
              files.map { f =>
                IO.arrayProducer(f._3)(LocalFileSystem.writer(Path(s"${dataPath}/products/${p.head}/${f._1}")))
              }) { d => log.debug("Write files: " + d) }
            service(_(created.withJsonBody("{\"pid\": \"" + p.head + "\"}")))
          case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            service(_(serverError))
        }

      case (_, Invalid(msgs)) =>
        log.debug("Send FAIL")
        implicit val l = r.language
        validationFail(msgs)

    }

  }

  private def extract(implicit loc: Language, id: Option[String], fieldPrefix: String, multipart: MultiPartBody): (List[(String, String, Array[Byte])], Validation[ProductDetail, FieldError]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    def stockFunc(in: String) = if (in.isEmpty()) None else Option(in.toInt)

    val params = extractParams(text)
    val files = extractProductBins(bins)

    val product = ((ProductDetail.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val productFormlet = Validator(product) <*>
      Validator(validateMapField(fieldPrefix + "title", ?("title").text)) <*>
      Validator(validateMapField(fieldPrefix + "description", ?("description").text)) <*>
      Validator(validateSpecs(fieldPrefix)) <*>
      Validator(validateDouble(fieldPrefix + "price", ?("price").text)) <*>
      Validator(validateOptional(fieldPrefix + "discount_price", s => Option(s.toDouble))) <*>
      Validator(validateDefault(0)) <*>
      Validator(validateOptional(fieldPrefix + "position", s => Option(s.toInt))) <*>
      Validator(validateOptional(fieldPrefix + "presentation_position", s => Option(s.toInt))) <*>
      Validator(validateBoolean(fieldPrefix + "unique", ?("unique.product").text)) <*>
      Validator(validateOptional(fieldPrefix + "stock", stockFunc)) <*>
      Validator(validateListField(fieldPrefix + "categories", ?("categories").text)) <*>
      Validator(validateDefault(Nil)) <*>
      Validator(optionalListField(fieldPrefix + "keywords", ?("keywords").text))

    try {
      (files, productFormlet validate params flatMap {
        case p @ ProductDetail(_,
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
