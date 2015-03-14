package net.shop
package web.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.DELETE
import net.shift.engine.http.MultiPartBody
import net.shift.engine.http.POST
import net.shift.engine.http.Resp
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
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.Selectors
import net.shop.api.ProductDetail
import net.shop.model.FieldError
import net.shop.model.ValidationFail
import net.shop.web.ShopApplication
import net.shop.web.services.FormImplicits.failSemigroup
import net.shift.common.TimeUtils._
import net.shift.io.FileSystem
import net.shift.io.FileOps
import net.shift.io.IO

object ProductService extends ShiftUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  def deleteProduct(implicit fs: FileSystem) = for {
    r <- DELETE
    Path("product" :: "delete" :: id :: Nil) <- path
    user <- auth
  } yield {
    ShopApplication.persistence.deleteProducts(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"data/products/$id"))
        service(_(Resp.ok))
      case scala.util.Failure(t) => service(_(Resp.notFound))
    }
  }

  def updateProduct(implicit fs: FileSystem) = for {
    r <- POST
    Path("product" :: "update" :: pid :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp) match {
      case (files, Valid(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)

        ShopApplication.persistence.productById(pid) match {
          case scala.util.Success(p) =>
            val merged = cpy.copy(images = p.images ++ cpy.images)

            ShopApplication.persistence.updateProducts(merged) match {
              case scala.util.Success(p) =>
                files.map { f =>
                  IO.arrayProducer(f._3)(FileOps.writer(Path(s"data/products/${p.head}/${f._1}")))
                }
                service(_(Resp.created))
              case scala.util.Failure(t) =>
                error("Cannot create product ", t)
                service(_(Resp.serverError))
            }

          case scala.util.Failure(f) => service(_(Resp.notFound))
        }

      case (_, Invalid(msgs)) => validationFail(msgs)(r.language.name)
    }
  }

  def createProduct(implicit fs: FileSystem) = for {
    r <- POST
    Path("product" :: "create" :: Nil) <- path
    user <- auth
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
                  IO.arrayProducer(f._3)(FileOps.writer(Path(s"data/products/${p.head}/${f._1}")))
                }) { d => log.debug("Write files: " + d) }
            }
            log.debug("Send OK")
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            log.debug("Send ERROR")
            service(_(Resp.serverError))
        }

      case (_, Invalid(msgs)) =>
        log.debug("Send FAIL")
        validationFail(msgs)(r.language.name)

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
      inputText(fieldPrefix + "properties")(validateProps(?("properties").text)) <*>
      inputDouble(fieldPrefix + "price")(validateDouble(fieldPrefix + "price", ?("price").text)) <*>
      inputOptional(fieldPrefix + "discount_price")(validateOptional(fieldPrefix + "discount_price", s => Option(s.toDouble))) <*>
      inputInt(fieldPrefix + "soldCount")(validateDefault(0)) <*>
      inputCheck(fieldPrefix + "unique", "false")(validateBoolean(fieldPrefix + "unique", ?("unique.product").text)) <*>
      inputOptional(fieldPrefix + "stock")(validateOptional(fieldPrefix + "stock", stockFunc)) <*>
      inputSelect(fieldPrefix + "categories", Nil)(validateListField(fieldPrefix + "categories", ?("categories").text)) <*>
      inputFile("files")(validateDefault(Nil)) <*>
      inputText(fieldPrefix + "keywords")(optionalListField(fieldPrefix + "keywords", ?("keywords").text))

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
        _) if (discountPrice >= price) => Invalid(ValidationFail(FieldError("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text)))
      case p => Valid(p)
    })
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

}
