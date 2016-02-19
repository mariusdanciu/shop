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
import net.shift.engine.http.DELETE
import net.shift.engine.http.MultiPartBody
import net.shift.engine.http.POST
import net.shift.engine.http.Resp
import net.shift.engine.http.Response.augmentResponse
import net.shift.engine.utils.ShiftUtils
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet.inputCheck
import net.shift.html.Formlet.inputDouble
import net.shift.html.Formlet.inputFile
import net.shift.html.Formlet.inputInt
import net.shift.html.Formlet.inputOptional
import net.shift.html.Formlet.inputSelect
import net.shift.html.Formlet.inputText
import net.shift.html.Invalid
import net.shift.html.Valid
import net.shift.html.Validation
import net.shift.io.FileOps
import net.shift.io.FileSystem
import net.shift.io.IO
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.Selectors
import net.shop.api.ProductDetail
import net.shop.api.ShopError
import net.shop.model.FieldError
import net.shop.model.ValidationFail
import net.shop.web.ShopApplication
import net.shop.web.services.FormImplicits.failSemigroup
import utils.ShopUtils.dataPath
import net.shift.io.Configs

class ProductService(implicit val cfg: Config) extends ShiftUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService 
  with Configs{

  def deleteProduct(implicit fs: FileSystem) = for {
    r <- DELETE
    Path(_, "product" :: "delete" :: id :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    ShopApplication.persistence.deleteProducts(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"${dataPath}/products/$id"))
        service(_(Resp.ok))
      case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(Resp.notFound))
    }
  }

  def updateProduct(implicit fs: FileSystem) = for {
    r <- POST
    Path(_, "product" :: "update" :: pid :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp) match {
      case (files, Valid(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)

        (for {
          p <- ShopApplication.persistence.productById(pid)
          u <- ShopApplication.persistence.updateProducts(cpy.copy(images = p.images ++ cpy.images))
        } yield {
          files.map { f =>
            IO.arrayProducer(f._3)(FileOps.writer(Path(s"${dataPath}/products/${u.head}/${f._1}")))
          }
          service(_(Resp.created))
        }) match {
          case scala.util.Success(s)                 => s
          case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t)                 => service(_(Resp.serverError))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)
    }
  }

  def createProduct(implicit fs: FileSystem) = for {
    r <- POST
    Path(_, "product" :: "create" :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
    mp <- multipartForm
  } yield {
    val extracted = duration(extract(r.language, None, "create_", mp)) { d =>
      log.debug("Extraction : " + d)
    }

    extracted match {
      case (files, Valid(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)
        val create = duration(ShopApplication.persistence.createProducts(cpy)) { d =>
          log.debug("Persist : " + d)
        }

        create match {
          case scala.util.Success(p) =>
            Future {
              duration(
                files.map { f =>
                  IO.arrayProducer(f._3)(FileOps.writer(Path(s"${dataPath}/products/${p.head}/${f._1}")))
                }) { d => log.debug("Write files: " + d) }
            }
            service(_(Resp.created))
          case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            service(_(Resp.serverError))
        }

      case (_, Invalid(msgs)) =>
        log.debug("Send FAIL")
        implicit val l = r.language
        validationFail(msgs)

    }

  }

  private def extract(implicit loc: Language, id: Option[String], fieldPrefix: String, multipart: MultiPartBody): (List[(String, String, Array[Byte])], Validation[ValidationFail, ProductDetail]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    def stockFunc(in: String) = if (in.isEmpty()) None else Option(in.toInt)

    val params = extractParams(text)
    val files = extractProductBins(bins)

    val product = ((ProductDetail.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val productFormlet = Formlet(product) <*>
      inputText(fieldPrefix + "title")(validateMapField(fieldPrefix + "title", ?("title").text)) <*>
      inputText(fieldPrefix + "description")(validateMapField(fieldPrefix + "description", ?("description").text)) <*>
      inputText(fieldPrefix + "properties")(validateProps) <*>
      inputText(fieldPrefix + "options")(validateOptions) <*>
      inputText(fieldPrefix + "userText")(validateUserText) <*>
      inputDouble(fieldPrefix + "price")(validateDouble(fieldPrefix + "price", ?("price").text)) <*>
      inputOptional(fieldPrefix + "discount_price")(validateOptional(fieldPrefix + "discount_price", s => Option(s.toDouble))) <*>
      inputInt(fieldPrefix + "soldCount")(validateDefault(0)) <*>
      inputOptional(fieldPrefix + "position")(validateOptional(fieldPrefix + "position", s => Option(s.toInt))) <*>
      inputOptional(fieldPrefix + "presentation_position")(validateOptional(fieldPrefix + "presentation_position", s => Option(s.toInt))) <*>
      inputCheck(fieldPrefix + "unique", "false")(validateBoolean(fieldPrefix + "unique", ?("unique.product").text)) <*>
      inputOptional(fieldPrefix + "stock")(validateOptional(fieldPrefix + "stock", stockFunc)) <*>
      inputSelect(fieldPrefix + "categories", Nil)(validateListField(fieldPrefix + "categories", ?("categories").text)) <*>
      inputFile("files")(validateDefault(Nil)) <*>
      inputText(fieldPrefix + "keywords")(optionalListField(fieldPrefix + "keywords", ?("keywords").text))

    try {
      (files, productFormlet validate params flatMap {
        case p @ ProductDetail(_,
          _,
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
          _) if (discountPrice >= price) => Invalid(ValidationFail(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text)))
        case p => Valid(p)
      })
    } catch {
      case e: Exception =>
        e.printStackTrace()
        (Nil, Invalid(ValidationFail(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text))))
    }
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

}
