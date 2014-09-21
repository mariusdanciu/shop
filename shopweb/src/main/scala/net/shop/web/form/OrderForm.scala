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

object OrderForm extends ShopUtils {
  sealed trait EnvValue
  case class FormField(value: String) extends EnvValue
  case class OrderItems(l: List[(ProductDetail, Int)]) extends EnvValue

  def validName(name: String, title: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(FormField(n)) if !n.isEmpty() => Success(n)
      case Some(FormField(n)) if n.isEmpty() => required
      case _ => required
    }
  }

  def validEmail(name: String, id: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.email").text)).text)))

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
    val required = Failure(List((id, Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.phone").text)).text)))

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
      case _ => Failure(List(("items", Loc.loc0(lang)("order.items.required").text)))
    }

  def validTerms(id: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], Boolean] =
    env => env.get("terms") match {
      case Some(FormField("on")) => Success(true)
      case _ => Failure(List((id, Loc.loc0(lang)("terms.and.conds.err").text)))
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
    val personFormlet = Formlet(person) <*>
      inputText("lname")(validName("fname", ?("order.first.name").text)) <*>
      inputText("fname")(validName("lname", ?("order.last.name").text))

    Formlet(order) <*>
      personFormlet <*>
      inputText("region")(validName("region", ?("order.region").text)) <*>
      inputText("city")(validName("city", ?("order.city").text)) <*>
      inputText("address")(validName("address",  ?("order.address").text)) <*>
      inputText("email")(validEmail("email", "email")) <*>
      inputText("phone")(validPhone("phone", "phone")) <*>
      inputCheck("terms", "true")(validTerms("terms")) <*>
      inputItems("items")(validItems)
  }

  def companyForm(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(uuid)
    val ? = Loc.loc0(lang) _

    val company = (Company.apply _).curried
    val companyFormlet = Formlet(company) <*>
      inputText("cname")(validName("cname",  ?("order.company.name").text)) <*>
      inputText("cif")(validName("cif", ?("order.company.cif").text)) <*>
      inputText("cregcom")(validName("cregcom", ?("order.company.reg.com").text)) <*>
      inputText("cbank")(validName("cbank", ?("order.company.bank").text)) <*>
      inputText("cbankaccount")(validName("cbankaccount",  ?("order.company.bank.account").text))

    Formlet(order) <*>
      companyFormlet <*>
      inputText("cregion")(validName("cregion", ?("order.region").text)) <*>
      inputText("ccity")(validName("ccity", ?("order.city").text)) <*>
      inputText("caddress")(validName("caddress", ?("order.address").text)) <*>
      inputText("cemail")(validEmail("cemail", "cemail")) <*>
      inputText("cphone")(validPhone("cphone", "cphone")) <*>
      inputCheck("cterms", "true")(validTerms("cterms")) <*>
      inputItems("items")(validItems)
  }

}

