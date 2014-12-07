package net.shop
package web.services

import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.POST
import net.shift.engine.http.Resp
import net.shift.engine.http.TextPart
import net.shift.engine.utils.ShiftUtils
import net.shift.template.Selectors
import net.shift.engine.http.MultiPartBody
import net.shop.api.ProductDetail
import net.shift.engine.http.Header
import net.shift.common.FileSplit
import javax.mail.Multipart
import net.shift.engine.http.MultiPart
import net.shift.loc.Loc
import net.shift.html.Formlet
import net.shift.loc.Language
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet._
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Failure
import net.shift.html.Success
import net.shift.html.Validation
import net.shift.js._
import net.shift.engine.http.JsResponse
import JsDsl._
import net.shift.engine.ShiftApplication
import net.shop.web.ShopApplication

object ProductCreateService extends PathUtils with ShiftUtils with Selectors with TraversingSpec with DefaultLog with ProductValidation {

  def createProduct = for {
    r <- POST
    Path("product" :: "create" :: Nil) <- path
    mp <- multipartForm
  } yield {
    extract(r.language, mp) match {
      case (files, net.shift.html.Success(o)) =>
        val cpy = o.copy(images = Set(files.map(f => f._2): _*).toList)
        println(cpy)
        ShopApplication.persistence.createProducts(cpy) match {
          case scala.util.Success(p) =>
            files.map { f =>
              scalax.file.Path.fromString(s"data/products/${p.head}/${f._1}").write(f._3)
            }
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create product ", t)
            service(_(Resp.created))
        }

      case (_, net.shift.html.Failure(msgs)) =>
        println(msgs)
        service(_(JsResponse(
          func() {
            JsStatement(
              (for {
                m <- msgs
              } yield {
                $(s"label[for='${m._1}']") ~
                  JsDsl.apply("css", "color", "#ff0000") ~
                  JsDsl.apply("attr", "title", m._2)
              }): _*)
          }.wrap.apply.toJsString)))

    }

  }

  private def extract(implicit loc: Language, multipart: MultiPartBody): (List[(String, String, Array[Byte])], Validation[ValidationError, ProductDetail]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    val params = extractParams(text)
    val files = extractBins(bins)

    files.foreach(f => println(f._1))

    val product = ((ProductDetail.apply _).curried)(None)
    val ? = Loc.loc0(loc) _

    val productFormlet = (Formlet(product) <*>
      inputText("create_title")(validateMapField("create_title", ?("title").text)) <*>
      inputText("create_description")(validateMapField("create_description", ?("description").text)) <*>
      inputText("create_properties")(validateProps(?("properties").text)) <*>
      inputDouble("create_price")(validateDouble("create_price", ?("price").text))) <*>
      inputText("create_oldPrice")(validateDefault("create_oldPrice", None)) <*>
      inputInt("create_soldCount")(validateDefault("create_soldCount", 0)) <*>
      inputSelect("create_categories", Nil)(validateListField("create_categories", ?("categories").text)) <*>
      inputFile("images")(validateDefault("images", Nil)) <*>
      inputSelect("create_keywords", Nil)(validateListField("create_keywords", ?("keywords").text))

    (files, productFormlet validate params)
  }
 
  def extractParams(text: List[MultiPart]) = ((Map.empty: Map[String, List[String]]) /: text) {
    case (acc, TextPart(h, content)) =>
      (for {
        Header(_, _, par) <- h.get("Content-Disposition")
        name <- par.get("name")
      } yield {
        acc.get(name).map(v => acc + (name -> (v ++ List(content)))) getOrElse (acc + (name -> List(content)))
      }) getOrElse acc
    case (acc, _) => acc
  }

  def split(content: String) = content.split("\\s*,\\s*").toList

  def extractBins(bins: List[MultiPart]) = ((Nil: List[(String, String, Array[Byte])]) /: bins) {
    case (acc, BinaryPart(h, content)) =>
      (for {
        cd <- h.get("Content-Disposition")
        FileSplit(n, ext) <- cd.params.get("filename")
        FileSplit(name, _) <- Some(n)
      } yield {
        acc ++ List(if (n.endsWith(".thumb"))
          (s"thumb/$name.$ext", s"$name.$ext", content)
        else if (n.endsWith(".normal"))
          (s"normal/$name.$ext", s"$name.$ext", content)
        else
          (s"large/$name.$ext", s"$name.$ext", content))
      }) getOrElse acc
    case (acc, _) => acc
  }

}
