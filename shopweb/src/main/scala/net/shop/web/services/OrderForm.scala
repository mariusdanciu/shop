package net.shop
package web.services

import scala.xml.NodeSeq
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.utils.ShopUtils._
import net.shop.api.ProductDetail
import net.shop.api.Person
import net.shop.api.Company
import net.shop.api.Order
import net.shop.api.Address
import net.shop.model.ValidationFail
import net.shop.model.FieldError
import net.shop.api.Transport
import net.shift.io.IODefaults
import net.shift.common.Validation
import net.shift.common.Valid
import net.shift.common.Invalid
import net.shift.common.Validator
import net.shift.io.IODefaults
import net.shift.io.LocalFileSystem

object OrderForm {

  type ValidationInput = Map[String, EnvValue]
  type ValidationFunc[T] = ValidationInput => Validation[T, FieldError]

  sealed trait EnvValue
  case class FormField(value: String) extends EnvValue
  case class OrderItems(l: List[(ProductDetail, Int)]) extends EnvValue
  case object NotFound extends EnvValue

  implicit val fs = LocalFileSystem

  def reqStr[T](name: String, title: String, f: String => Validation[T, FieldError])(implicit lang: Language): ValidationFunc[T] =
    env => {
      val failed = Invalid(List(FieldError(name, Loc.loc(lang)("field.required", Seq(title)).text)))

      env.get(name) match {
        case Some(FormField(e)) if (!e.isEmpty()) => f(e)
        case _                                    => failed
      }
    }

  def validEmail(name: String, id: String)(implicit lang: Language): ValidationFunc[String] = env => {
    val required = Invalid(List(FieldError(id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("email").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(email)) => if (email.matches("""([\w\.\_]+)@([\w\.]+)"""))
        Valid(email)
      else
        Invalid(List(FieldError(id, Loc.loc(lang)("invalid.email", Seq(email)).text)))
      case _ => required
    }
  }

  def validPhone(name: String, id: String)(implicit lang: Language): ValidationFunc[String] = env => {
    val required = Invalid(List(FieldError(id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("phone").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(phone)) => if (phone.matches("""[0-9]+"""))
        Valid(phone)
      else
        Invalid(List(FieldError(id, Loc.loc(lang)("invalid.phone", Seq(phone)).text)))
      case _ => required
    }
  }

  def validItems(implicit lang: Language): ValidationFunc[List[(ProductDetail, Int)]] =
    env => env.get("items") match {
      case Some(OrderItems(l)) => Valid(l)
      case _                   => Invalid(List(FieldError("items", Loc.loc0(lang)("order.items.required").text)))
    }

  def validTerms(id: String)(implicit lang: Language): ValidationFunc[Boolean] =
    env => env.get("terms") match {
      case Some(FormField("on")) => Valid(true)
      case _                     => Invalid(List(FieldError(id, Loc.loc0(lang)("terms.and.conds.err").text)))
    }

  def validTransport(id: String)(implicit lang: Language): ValidationFunc[Transport] =
    env => env.get("transport") match {
      case Some(FormField("19.99")) => Valid(Transport(Loc.loc0(lang)("transport.1").text, 19.99f))
      case Some(FormField("9.99"))  => Valid(Transport(Loc.loc0(lang)("transport.2").text, 9.99f))
      case _                        => Invalid(List(FieldError(id, "error")))
    }

  def inputItems(name: String)(f: ValidationInput => Validation[List[(ProductDetail, Int)], FieldError]) =
    new Validator[List[(ProductDetail, Int)], Map[String, EnvValue], FieldError] {
      def validate = f
    }

  def form(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val person = (Person.apply _).curried
    val address = ((Address.apply _).curried)(None)("destination")("Romania")

    val personFormlet = (Validator(person) <*>
      Validator(reqStr("fname", ?("first.name").text, Valid(_)))) <*>
      Validator(reqStr("lname", ?("last.name").text, Valid(_))) <*>
      Validator(reqStr("cnp", ?("cnp").text, Valid(_)))

    val addressFormlet = Validator(address) <*>
      Validator(reqStr("region", ?("region").text, Valid(_))) <*>
      Validator(reqStr("city", ?("city").text, Valid(_))) <*>
      Validator(reqStr("address", ?("address").text, Valid(_))) <*>
      Validator(reqStr("zip", ?("zip").text, Valid(_)))

    Validator(order) <*>
      personFormlet <*>
      addressFormlet <*>
      Validator(validEmail("email", "email")) <*>
      Validator(validPhone("phone", "phone")) <*>
      Validator(validTerms("terms")) <*>
      Validator(validTransport("transport_pf")) <*>
      Validator(validItems)
  }

}

