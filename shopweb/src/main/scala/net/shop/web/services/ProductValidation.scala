package net.shop.web.services

import net.shift.html.Failure
import net.shift.html.Success
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc

trait ProductValidation {

  type ValidationError = List[(String, String)]
  type ValidationMap = Map[String, String]
  type ValidationList = List[String]
  type ValidationInput = Map[String, List[String]]

  implicit val o = new Ordering[Double] {
    def compare(l: Double, r: Double): Int = (l - r).toInt
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
      case Some(n :: Nil) if !n.isEmpty => Success(n.split("\\s*,\\s*").toList)
      case Some(n) if !n.isEmpty        => Success(n)
      case Some(n) if n.isEmpty         => failed
      case _                            => failed
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

  def validateOptional[T](name: String, f: String => Option[T])(implicit lang: Language): ValidationInput => Validation[ValidationError, Option[T]] = env => {
    env.get(name) match {
      case Some(n :: _) if !n.isEmpty => Success(f(n))
      case _                          => Success(None)
    }
  }

  def validateDiscount[T: Ordering](name: String, f: String => Option[T])(implicit lang: Language): ValidationInput => Validation[ValidationError, Option[T]] = env => {
    val failed = Failure(List((name, Loc.loc0(lang)("field.discount.smaller").text)))
    val v = implicitly[Ordering[T]]

    (env.get("edit_discount_price"), env.get("edit_price")) match {
      case (Some(n :: _), Some(p :: _)) if !n.isEmpty =>
        (f(n), f(p)) match {
          case (Some(d), Some(current)) if (v.compare(d, current) < 0) => Success(Some(d))
          case _ => failed
        }
      case _ => Success(None)
    }
  }

  def validateDefault[S](name: String, v: S)(implicit lang: Language): ValidationInput => Validation[ValidationError, S] =
    env => Success(v)

}