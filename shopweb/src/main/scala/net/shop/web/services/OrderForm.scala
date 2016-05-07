package net.shop
package web.services

import scala.xml.NodeSeq
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet._
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Validation
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
import net.shop.web.services.FormImplicits._
import net.shift.html.Valid
import net.shift.html.Invalid
import net.shop.api.Transport

object OrderForm {

  type ValidationInput = Map[String, EnvValue]
  type ValidationFunc[T] = ValidationInput => Validation[ValidationFail, T]

  sealed trait EnvValue
  case class FormField(value: String) extends EnvValue
  case class OrderItems(l: List[(ProductDetail, Int)]) extends EnvValue
  case object NotFound extends EnvValue

  def reqStr[T](name: String, title: String, f: String => Validation[ValidationFail, T])(implicit lang: Language): ValidationFunc[T] =
    env => {
      val failed = Invalid(ValidationFail(FieldError(name, Loc.loc(lang)("field.required", Seq(title)).text)))

      env.get(name) match {
        case Some(FormField(e)) if (!e.isEmpty()) => f(e)
        case _                                    => failed
      }
    }

  def validEmail(name: String, id: String)(implicit lang: Language): ValidationFunc[String] = env => {
    val required = Invalid(ValidationFail(FieldError(id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("email").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(email)) => if (email.matches("""([\w\.\_]+)@([\w\.]+)"""))
        Valid(email)
      else
        Invalid(ValidationFail(FieldError(id, Loc.loc(lang)("invalid.email", Seq(email)).text)))
      case _ => required
    }
  }

  def validPhone(name: String, id: String)(implicit lang: Language): ValidationFunc[String] = env => {
    val required = Invalid(ValidationFail(FieldError(id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("phone").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(phone)) => if (phone.matches("""[0-9]+"""))
        Valid(phone)
      else
        Invalid(ValidationFail(FieldError(id, Loc.loc(lang)("invalid.phone", Seq(phone)).text)))
      case _ => required
    }
  }

  def validItems(implicit lang: Language): ValidationFunc[List[(ProductDetail, Int)]] =
    env => env.get("items") match {
      case Some(OrderItems(l)) => Valid(l)
      case _                   => Invalid(ValidationFail(FieldError("items", Loc.loc0(lang)("order.items.required").text)))
    }

  def validTerms(id: String)(implicit lang: Language): ValidationFunc[Boolean] =
    env => env.get("terms") match {
      case Some(FormField("on")) => Valid(true)
      case _                     => Invalid(ValidationFail(FieldError(id, Loc.loc0(lang)("terms.and.conds.err").text)))
    }

  def validTransport(id: String)(implicit lang: Language): ValidationFunc[Transport] =
    env => env.get("transport") match {
      case Some(FormField("1")) => Valid(Transport(Loc.loc0(lang)("transport.1").text, 19.99f))
      case Some(FormField("2")) => Valid(Transport(Loc.loc0(lang)("transport.2").text, 9.99f))
      case _                    => Invalid(ValidationFail(FieldError(id, "error")))
    }

  def inputItems(name: String)(f: ValidationInput => Validation[ValidationFail, List[(ProductDetail, Int)]]) =
    new Formlet[List[(ProductDetail, Int)], Map[String, EnvValue], ValidationFail] {
      val validate = f
      override def html = NodeSeq.Empty;
    }

  def form(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val person = (Person.apply _).curried
    val address = ((Address.apply _).curried)(None)("destination")("Romania")

    val personFormlet = Formlet(person) <*>
      inputText("lname")(reqStr("fname", ?("first.name").text, Valid(_))) <*>
      inputText("fname")(reqStr("lname", ?("last.name").text, Valid(_))) <*>
      inputText("cnp")(reqStr("cnp", ?("cnp").text, Valid(_)))

    val addressFormlet = Formlet(address) <*>
      inputText("region")(reqStr("region", ?("region").text, Valid(_))) <*>
      inputText("city")(reqStr("city", ?("city").text, Valid(_))) <*>
      inputText("address")(reqStr("address", ?("address").text, Valid(_))) <*>
      inputText("zip")(reqStr("zip", ?("zip").text, Valid(_)))

    Formlet(order) <*>
      personFormlet <*>
      addressFormlet <*>
      inputText("email")(validEmail("email", "email")) <*>
      inputText("phone")(validPhone("phone", "phone")) <*>
      inputCheck("terms", "true")(validTerms("terms")) <*>
      inputSelect("transport", Nil)(validTransport("transport_pf")) <*>
      inputItems("items")(validItems)
  }

  def companyForm(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val company = (Company.apply _).curried
    val address = ((Address.apply _).curried)(None)("destination")("Romania")

    val companyFormlet = Formlet(company) <*>
      inputText("cname")(reqStr("cname", ?("company.name").text, Valid(_))) <*>
      inputText("cif")(reqStr("cif", ?("company.cif").text, Valid(_))) <*>
      inputText("cregcom")(reqStr("cregcom", ?("company.reg.com").text, Valid(_))) <*>
      inputText("cbank")(reqStr("cbank", ?("company.bank").text, Valid(_))) <*>
      inputText("cbankaccount")(reqStr("cbankaccount", ?("company.bank.account").text, Valid(_)))

    val addressFormlet = Formlet(address) <*>
      inputText("cregion")(reqStr("cregion", ?("region").text, Valid(_))) <*>
      inputText("ccity")(reqStr("ccity", ?("city").text, Valid(_))) <*>
      inputText("caddress")(reqStr("caddress", ?("address").text, Valid(_))) <*>
      inputText("czip")(reqStr("czip", ?("zip").text, Valid(_)))

    Formlet(order) <*>
      companyFormlet <*>
      addressFormlet <*>
      inputText("cemail")(validEmail("cemail", "cemail")) <*>
      inputText("cphone")(validPhone("cphone", "cphone")) <*>
      inputCheck("cterms", "true")(validTerms("cterms")) <*>
      inputSelect("transport", Nil)(validTransport("transport_pj")) <*>
      inputItems("items")(validItems)
  }

}

