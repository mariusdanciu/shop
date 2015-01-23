package net.shop.web.services

import net.shift.common.FileSplit
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.Header
import net.shift.engine.http.JsResponse
import net.shift.engine.http.MultiPart
import net.shift.engine.http.TextPart
import net.shift.html.Failure
import net.shift.html.Success
import net.shift.html.Validation
import net.shift.js._
import net.shift.js.JsDsl._
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.web.ShopApplication

trait FormValidation {

  type ValidationError = List[(String, String)]
  type ValidationMap = Map[String, String]
  type ValidationList = List[String]
  type ValidationInput = Map[String, List[String]]
  type ValidationFunc[T] = ValidationInput => Validation[ValidationError, T]

  implicit val o = new Ordering[Double] {
    def compare(l: Double, r: Double): Int = (l - r).toInt
  }

  def required[T](name: String, title: String, f: String => Validation[ValidationError, T])(implicit lang: Language): ValidationFunc[T] =
    env => {
      val failed = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

      env.get(name) match {
        case Some(n :: _) if !n.isEmpty => f(n)
        case Some(n) if n.isEmpty       => failed
        case _                          => failed
      }
    }

  def optional[T](name: String, title: String, default: => T, f: String => Validation[ValidationError, T])(implicit lang: Language): ValidationFunc[T] =
    env => {
      env.get(name) match {
        case Some(n :: _) if !n.isEmpty => f(n)
        case Some(n) if n.isEmpty       => Success(default)
        case _                          => Success(default)
      }
    }

  def validateProps(title: String)(implicit lang: Language): ValidationFunc[ValidationMap] =
    env => {
      (env.get("pkey"), env.get("pval")) match {
        case (Some(k), Some(v)) => Success(k.zip(v).toMap)
        case _                  => Success(Map.empty)
      }
    }

  def validateMapField(name: String, title: String)(implicit lang: Language): ValidationFunc[ValidationMap] =
    required(name, title, s => Success(Map(lang.language -> s)))

  def validateListField(name: String, title: String)(implicit lang: Language): ValidationFunc[ValidationList] =
    required(name, title, s => Success(s.split("\\s*,\\s*").toList))

  def validateDouble(name: String, title: String)(implicit lang: Language): ValidationFunc[Double] =
    required(name, title, s => Success(s.toDouble))

  def validateText(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, String] =
    optional(name, title, "", Success(_))

  def validateCreateUser(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[ValidationError, String] =
    required(name, title, s => ShopApplication.persistence.userByEmail(s) match {
      case scala.util.Success(email) => Failure(List((name, Loc.loc0(lang)("user.already.exists").text)))
      case _                         => Success(s)
    })

  def validateOptional[T](name: String, f: String => Option[T])(implicit lang: Language): ValidationInput => Validation[ValidationError, Option[T]] = env => {
    env.get(name) match {
      case Some(n :: _) if !n.isEmpty => Success(f(n))
      case _                          => Success(None)
    }
  }

  def validateDefault[S](v: S)(implicit lang: Language): ValidationInput => Validation[ValidationError, S] =
    env => Success(v)

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

  def extractProductBins(bins: List[MultiPart]) = ((Nil: List[(String, String, Array[Byte])]) /: bins) {
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

  def extractCategoryBin(bins: MultiPart): Option[(String, Array[Byte])] = bins match {
    case BinaryPart(h, content) =>
      (for {
        cd <- h.get("Content-Disposition")
        FileSplit(n, ext) <- cd.params.get("filename")
        FileSplit(name, _) <- Some(n)
      } yield {
        (s"$name.$ext", content)
      })
    case _ => None
  }

  def validationFail(msgs: ValidationError) = service(_(JsResponse(
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

case class ValidationCombiner()