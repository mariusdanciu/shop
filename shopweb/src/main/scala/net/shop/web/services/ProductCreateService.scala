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

object ProductCreateService extends PathUtils with ShiftUtils with Selectors with TraversingSpec with DefaultLog {

  type ValidationError = List[(String, String)]
  type ValidationMap = Map[String, String]
  type ValidationList = List[String]
  type ValidationInput = Map[String, List[String]]

  def createProduct = for {
    r <- POST
    Path("product" :: "create" :: Nil) <- path
    mp <- multipartForm
  } yield {
    extract(r.language, mp) match {
      case net.shift.html.Success(o) =>
        println(o)
        service(_(Resp.created))
      case net.shift.html.Failure(msgs) =>
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

  private def extract(implicit loc: Language, multipart: MultiPartBody): Validation[ValidationError, ProductDetail] = {

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

    productFormlet validate params
  }

  def validateProps(title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, ValidationMap] = env => {
    (env.get("pkey"), env.get("pval")) match {
      case (Some(k), Some(v)) => Success(k.zip(v).toMap)
      case _                  => Success(Map.empty)
    }
  }

  def validateMapField(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, ValidationMap] = env => {
    val failed = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(n :: _) if !n.isEmpty => Success(Map(lang.language -> n))
      case Some(n) if n.isEmpty       => failed
      case _                          => failed
    }
  }

  def validateListField(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, ValidationList] = env => {
    val failed = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))
    env.get(name) match {
      case Some(n) if !n.isEmpty => Success(n)
      case Some(n) if n.isEmpty  => failed
      case _                     => failed
    }
  }

  def validateDouble(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, Double] = env => {
    val failed = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(n :: _) if !n.isEmpty => Success(n.toDouble)
      case Some(n) if n.isEmpty       => failed
      case _                          => failed
    }
  }

  def validateDefault[S](name: String, v: S)(implicit lang: Language): ValidationInput => Validation[ValidationError, S] =
    env => Success(v)

  def extractParams(text: List[MultiPart]) = ((Map.empty: Map[String, List[String]]) /: text) {
    case (acc, TextPart(h, content)) =>
      (for {
        Header(_, _, par) <- h.get("Content-Disposition")
        name <- par.get("name")
      } yield {
        acc.get(name).map(v => acc + (name -> (v ++ List(content)))) getOrElse (acc + (name -> content.split("\\s*,\\s*").toList))
      }) getOrElse acc
    case (acc, _) => acc
  }

  def extractBins(bins: List[MultiPart]) = ((Nil: List[(String, Array[Byte])]) /: bins.zipWithIndex) {
    case (acc, (BinaryPart(h, content), idx)) =>
      (for {
        cd <- h.get("Content-Disposition")
        FileSplit(name, ext) <- cd.params.get("filename")
      } yield {
        acc ++ List(if (name.endsWith(".thumb"))
          (s"thumb/$idx.$ext", content)
        else if (name.endsWith(".normal"))
          (s"normal/$idx.$ext", content)
        else
          (s"large/$idx.$ext", content))
      }) getOrElse acc
    case (acc, _) => acc
  }

}
