package net.shop.web.services

import net.shift.common.FileSplit
import net.shift.common.Semigroup
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.Header
import net.shift.engine.http.JsonResponse
import net.shift.engine.http.MultiPart
import net.shift.engine.http.Response.augmentResponse
import net.shift.engine.http.TextPart
import net.shift.io.IODefaults
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.api.Formatter
import net.shop.model.FieldError
import net.shop.model.Formatters.ValidationErrorWriter
import net.shop.web.ShopApplication
import net.shift.io.FileSystem
import net.shop.model.ValidationFail
import net.shift.common.Valid
import net.shift.common.Validation
import net.shift.common.Invalid

object FormImplicits extends IODefaults {
  implicit val o = new Ordering[Double] {
    def compare(l: Double, r: Double): Int = (l - r).toInt
  }

}

trait FormValidation extends IODefaults with ServiceDependencies {

  type ValidationMap = Map[String, String]
  type ValidationList = List[String]
  type ValidationInput = Map[String, List[String]]
  type ValidationFunc[T] = ValidationInput => Validation[T, FieldError]

  def required[T](name: String, title: String, f: String => Validation[T, FieldError])(implicit lang: Language): ValidationFunc[T] =
    env => {
      val failed = Invalid(List(FieldError(name, Loc.loc(lang)("field.required", Seq(title)).text)))

      env.get(name) match {
        case Some(n :: _) if !n.isEmpty => f(n)
        case Some(n) if n.isEmpty       => failed
        case _                          => failed
      }
    }

  def optional[T](name: String, title: String, default: => T, f: String => Validation[T, FieldError])(implicit lang: Language): ValidationFunc[T] =
    env => {
      env.get(name) match {
        case Some(n :: _) if !n.isEmpty => f(n)
        case Some(n) if n.isEmpty       => Valid(default)
        case _                          => Valid(default)
      }
    }

  def validateProps(implicit lang: Language): ValidationFunc[ValidationMap] =
    env => {
      (env.get("pkey"), env.get("pval")) match {
        case (Some(k), Some(v)) => Valid(k.zip(v).toMap)
        case _                  => Valid(Map.empty)
      }
    }

  def validateMapField(name: String, title: String)(implicit lang: Language): ValidationFunc[ValidationMap] =
    required(name, title, s => Valid(Map(lang.name -> s)))

  def validateListField(name: String, title: String)(implicit lang: Language): ValidationFunc[ValidationList] =
    required(name, title, s => Valid(s.split("\\s*,\\s*").toList))

  def optionalListField(name: String, title: String)(implicit lang: Language): ValidationFunc[ValidationList] =
    optional(name, title, Nil, s => Valid(s.split("\\s*,\\s*").toList))

  def validateInt(name: String, title: String)(implicit lang: Language): ValidationFunc[Int] =
    required(name, title, s =>
      try {
        Valid(s.toInt)
      } catch {
        case e: Exception => Invalid(List(FieldError(name, Loc.loc(lang)("field.incorrect", Seq(title)).text)))
      })

  def validateBoolean(name: String, title: String)(implicit lang: Language): ValidationFunc[Boolean] =
    optional(name, title, false, s => {
      Valid(!s.isEmpty())
    })

  def validateDouble(name: String, title: String)(implicit lang: Language): ValidationFunc[Double] =
    required(name, title, s =>
      try {
        val d = s.toDouble
        Valid(d)
      } catch {
        case e: Exception => Invalid(List(FieldError(name, Loc.loc(lang)("field.incorrect", Seq(title)).text)))
      })

  def validateText(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[String, FieldError] =
    optional(name, title, "", Valid(_))

  def validateCreateUser(name: String, title: String)(implicit lang: Language): ValidationInput => Validation[String, FieldError] =
    required(name, title, s => store.userByEmail(s) match {
      case scala.util.Success(Some(email)) => Invalid(List(FieldError(name, Loc.loc0(lang)("user.already.exists").text)))
      case _                               => Valid(s)
    })

  def validateOptional[T](name: String, f: String => Option[T])(implicit lang: Language): ValidationInput => Validation[Option[T], FieldError] = env => {
    env.get(name) match {
      case Some(n :: _) if !n.isEmpty => Valid(f(n))
      case _                          => Valid(None)
    }
  }

  def validateDefault[S](v: S)(implicit lang: Language): ValidationInput => Validation[S, FieldError] =
    env => Valid(v)

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
      } yield {
        (s"$n.$ext", content)
      })
    case _ => None
  }

  def validationFail(msgs: List[FieldError])(implicit lang: Language, fs: FileSystem) =
    service(r => {
      r(JsonResponse(Formatter.format(msgs)).code(403))
    })

  def respValidationFail(resp: net.shift.engine.http.AsyncResponse, msgs: List[FieldError])(implicit lang: Language, fs: FileSystem) =
    resp(JsonResponse(Formatter.format(msgs)).code(403))

}

