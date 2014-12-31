package net.shop
package web.services

import net.shift.common.DefaultLog
import net.shift.common.FileSplit
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.DELETE
import net.shift.engine.http.Header
import net.shift.engine.http.JsResponse
import net.shift.engine.http.MultiPart
import net.shift.engine.http.MultiPartBody
import net.shift.engine.http.POST
import net.shift.engine.http.Resp
import net.shift.engine.http.TextPart
import net.shift.engine.utils.ShiftUtils
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet.inputDouble
import net.shift.html.Formlet.inputFile
import net.shift.html.Formlet.inputInt
import net.shift.html.Formlet.inputOptional
import net.shift.html.Formlet.inputSelect
import net.shift.html.Formlet.inputText
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.Selectors
import net.shop.api.ProductDetail
import net.shop.web.ShopApplication
import net.shift.js._
import JsDsl._

object ProductWriteService extends PathUtils
  with ShiftUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  def deleteProduct = for {
    r <- DELETE
    Path("product" :: "delete" :: id :: Nil) <- path
    user <- auth
  } yield {
    ShopApplication.persistence.deleteProducts(id) match {
      case scala.util.Success(num) =>
        scalax.file.Path.fromString(s"data/products/$id").deleteRecursively(true);
        service(_(Resp.ok))
      case scala.util.Failure(t) => service(_(Resp.notFound))
    }
  }

  def updateProduct = for {
    r <- POST
    Path("product" :: "update" :: pid :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, Some(pid), "edit_", mp) match {
      case (files, net.shift.html.Success(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)

        ShopApplication.persistence.productById(pid) match {
          case scala.util.Success(p) =>
            val merged = cpy.copy(images = p.images ++ cpy.images)

            ShopApplication.persistence.updateProducts(merged) match {
              case scala.util.Success(p) =>
                files.map { f =>
                  scalax.file.Path.fromString(s"data/products/${p.head}/${f._1}").write(f._3)
                }
                service(_(Resp.created))
              case scala.util.Failure(t) =>
                error("Cannot create product ", t)
                service(_(Resp.serverError))
            }

          case scala.util.Failure(f) => service(_(Resp.notFound))
        }

      case (_, net.shift.html.Failure(msgs)) => validationFail(msgs)
    }
  }

  def createProduct = for {
    r <- POST
    Path("product" :: "create" :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, "create_", mp) match {
      case (files, net.shift.html.Success(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)
        ShopApplication.persistence.createProducts(cpy) match {
          case scala.util.Success(p) =>
            files.map { f =>
              scalax.file.Path.fromString(s"data/products/${p.head}/${f._1}").write(f._3)
            }
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            service(_(Resp.serverError))
        }

      case (_, net.shift.html.Failure(msgs)) => validationFail(msgs)

    }

  }

  private def extract(implicit loc: Language, id: Option[String], fieldPrefix: String, multipart: MultiPartBody): (List[(String, String, Array[Byte])], Validation[ValidationError, ProductDetail]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    val params = extractParams(text)
    val files = extractProductBins(bins)

    val product = ((ProductDetail.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val productFormlet = Formlet(product) <*>
      inputText(fieldPrefix + "title")(validateMapField(fieldPrefix + "title", ?("title").text)) <*>
      inputText(fieldPrefix + "description")(validateMapField(fieldPrefix + "description", ?("description").text)) <*>
      inputText(fieldPrefix + "properties")(validateProps(?("properties").text)) <*>
      inputDouble(fieldPrefix + "price")(validateDouble(fieldPrefix + "price", ?("price").text)) <*>
      inputOptional(fieldPrefix + "discount_price")(validateOptional(fieldPrefix + "discount_price", s => Some(s.toDouble))) <*>
      inputInt(fieldPrefix + "soldCount")(validateDefault(fieldPrefix + "soldCount", 0)) <*>
      inputSelect(fieldPrefix + "categories", Nil)(validateListField(fieldPrefix + "categories", ?("categories").text)) <*>
      inputFile("files")(validateDefault("files", Nil)) <*>
      inputSelect(fieldPrefix + "keywords", Nil)(validateListField(fieldPrefix + "keywords", ?("keywords").text))

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
        _) if (discountPrice >= price) => net.shift.html.Failure(List(("edit_discount_price", Loc.loc0(loc)("field.discount.smaller").text)))
      case p => net.shift.html.Success(p)
    })
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

}
