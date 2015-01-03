package net.shop
package web.form

import scala.xml.NodeSeq
import net.shift.html.Failure
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet._
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Success
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.api.Order
import scala.util.Random
import net.shop.utils.ShopUtils
import net.shop.api.ProductDetail
import net.shop.api.Person
import net.shop.api.Company
import net.shop.api.Address

object OrderForm extends ShopUtils {
  sealed trait EnvValue
  case class FormField(value: String) extends EnvValue
  case class OrderItems(l: List[(ProductDetail, Int)]) extends EnvValue

  def validName(name: String, title: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(FormField(n)) if !n.isEmpty() => Success(n)
      case Some(FormField(n)) if n.isEmpty()  => required
      case _                                  => required
    }
  }

  def validEmail(name: String, id: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("email").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(email)) => if (email.matches("""([\w\.\_]+)@([\w\.]+)"""))
        Success(email)
      else
        Failure(List((id, Loc.loc(lang)("invalid.email", Seq(email)).text)))
      case _ => required
    }
  }

  def validPhone(name: String, id: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("phone").text)).text)))

    env.get(name) match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(phone)) => if (phone.matches("""[0-9]+"""))
        Success(phone)
      else
        Failure(List((id, Loc.loc(lang)("invalid.phone", Seq(phone)).text)))
      case _ => required
    }
  }

  def validItems(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], List[(ProductDetail, Int)]] =
    env => env.get("items") match {
      case Some(OrderItems(l)) => Success(l)
      case _                   => Failure(List(("items", Loc.loc0(lang)("order.items.required").text)))
    }

  def validTerms(id: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], Boolean] =
    env => env.get("terms") match {
      case Some(FormField("on")) => Success(true)
      case _                     => Failure(List((id, Loc.loc0(lang)("terms.and.conds.err").text)))
    }

  def inputItems(name: String)(f: Map[String, EnvValue] => Validation[List[(String, String)], List[(ProductDetail, Int)]]) =
    new Formlet[List[(ProductDetail, Int)], Map[String, EnvValue], List[(String, String)]] {
      val validate = f
      override def html = NodeSeq.Empty;
    }

  def form(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val person = (Person.apply _).curried
    val address = ((Address.apply _).curried)(None)

    val personFormlet = Formlet(person) <*>
      inputText("lname")(validName("fname", ?("first.name").text)) <*>
      inputText("fname")(validName("lname", ?("last.name").text)) <*>
      inputText("cnp")(validName("cnp", ?("cnp").text))

    val addressFormlet = Formlet(address) <*>
      inputText("country")(validName("country", ?("country").text)) <*>
      inputText("region")(validName("region", ?("region").text)) <*>
      inputText("city")(validName("city", ?("city").text)) <*>
      inputText("address")(validName("address", ?("address").text)) <*>
      inputText("zip")(validName("zip", ?("zip").text))

    Formlet(order) <*>
      personFormlet <*>
      addressFormlet <*>
      inputText("email")(validEmail("email", "email")) <*>
      inputText("phone")(validPhone("phone", "phone")) <*>
      inputCheck("terms", "true")(validTerms("terms")) <*>
      inputItems("items")(validItems)
  }

  def companyForm(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val company = (Company.apply _).curried
    val address = ((Address.apply _).curried)(None)

    val companyFormlet = Formlet(company) <*>
      inputText("cname")(validName("cname", ?("company.name").text)) <*>
      inputText("cif")(validName("cif", ?("company.cif").text)) <*>
      inputText("cregcom")(validName("cregcom", ?("company.reg.com").text)) <*>
      inputText("cbank")(validName("cbank", ?("company.bank").text)) <*>
      inputText("cbankaccount")(validName("cbankaccount", ?("company.bank.account").text))

    val addressFormlet = Formlet(address) <*>
      inputText("ccountry")(validName("ccountry", ?("country").text)) <*>
      inputText("cregion")(validName("cregion", ?("region").text)) <*>
      inputText("ccity")(validName("ccity", ?("city").text)) <*>
      inputText("caddress")(validName("caddress", ?("address").text)) <*>
      inputText("czip")(validName("czip", ?("zip").text))

    Formlet(order) <*>
      companyFormlet <*>
      addressFormlet <*>
      inputText("cemail")(validEmail("cemail", "cemail")) <*>
      inputText("cphone")(validPhone("cphone", "cphone")) <*>
      inputCheck("cterms", "true")(validTerms("cterms")) <*>
      inputItems("items")(validItems)
  }

}

