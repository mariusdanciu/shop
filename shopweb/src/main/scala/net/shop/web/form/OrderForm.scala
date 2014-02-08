package net.shop
package web.form

import net.shift._
import net.shift.html._
import net.shift.html.Formlet._
import net.shift.loc._
import net.shop.model.Order
import scala.xml.NodeSeq

object OrderForm {

  def validName(name: String, err: String, title: String)(implicit lang: Language): Map[String, String] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(n) if !n.isEmpty() => Success(n)
      case Some(n) if n.isEmpty() => required
      case _ => required
    }
  }

  def validEmail(implicit lang: Language): Map[String, String] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List(("email", Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.email").text)).text)))

    env.get("email") match {
      case Some(n) if n.isEmpty() => required
      case Some(email) => if (email.matches("""(\w+)@([\w\.]+)"""))
        Success(email)
      else Failure(List(("email", Loc.loc(lang)("invalid.email", Seq(email)).text)))
      case _ => required
    }
  }

  def validPhone(implicit lang: Language): Map[String, String] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List(("phone", Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.phone").text)).text)))

    env.get("phone") match {
      case Some(n) if n.isEmpty() => required
      case Some(phone) => if (phone.matches("""[0-9]+"""))
        Success(phone)
      else Failure(List(("phone", Loc.loc(lang)("invalid.phone", Seq(phone)).text)))
      case _ => required
    }
  }

  def form(implicit lang: Language) = {
    val order = (Order.apply _).curried
    val ? = Loc.loc0(lang) _

    Formlet(order) <*>
      inputText("lname")(validName("lname", "invalid.name", ?("order.last.name").text)) <*>
      inputText("fname")(validName("fname", "invalid.name", ?("order.first.name").text)) <*>
      inputText("region")(validName("region", "invalid.name", ?("order.region").text)) <*>
      inputText("city")(validName("city", "invalid.name", ?("order.city").text)) <*>
      inputText("address")(validName("address", "invalid.name", ?("order.address").text)) <*>
      inputText("email")(validEmail) <*>
      inputText("phone")(validPhone)
  }
}

