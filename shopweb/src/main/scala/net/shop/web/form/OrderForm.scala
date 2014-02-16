package net.shop
package web.form

import scala.xml.NodeSeq
import net.shift.html.Failure
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet.inputText
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Success
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.model.Order
import scala.util.Random

object OrderForm {
  sealed trait EnvValue
  case class FormField(value: String) extends EnvValue
  case class OrderItems(l: List[(String, Int)]) extends EnvValue

  val random = new Random(0)
  
  def validName(name: String, err: String, title: String)(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List((name, Loc.loc(lang)("field.required", Seq(title)).text)))

    env.get(name) match {
      case Some(FormField(n)) if !n.isEmpty() => Success(n)
      case Some(FormField(n)) if n.isEmpty() => required
      case _ => required
    }
  }

  def validEmail(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List(("email", Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.email").text)).text)))

    env.get("email") match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(email)) => if (email.matches("""([\w\.\_]+)@([\w\.]+)"""))
        Success(email)
      else Failure(List(("email", Loc.loc(lang)("invalid.email", Seq(email)).text)))
      case _ => required
    }
  }

  def validPhone(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], String] = env => {
    val required = Failure(List(("phone", Loc.loc(lang)("field.required", Seq(Loc.loc0(lang)("order.phone").text)).text)))

    env.get("phone") match {
      case Some(FormField(n)) if n.isEmpty() => required
      case Some(FormField(phone)) => if (phone.matches("""[0-9]+"""))
        Success(phone)
      else Failure(List(("phone", Loc.loc(lang)("invalid.phone", Seq(phone)).text)))
      case _ => required
    }
  }

  def validItems(implicit lang: Language): Map[String, EnvValue] => Validation[List[(String, String)], List[(String, Int)]] =
    env => env.get("items") match {
      case Some(OrderItems(n)) => Success(n)
      case _ => Failure(List(("items", Loc.loc0(lang)("order.items.required").text)))
    }

  def inputItems[Err](name: String)(f: Map[String, EnvValue] => Validation[Err, List[(String, Int)]]) =
    new Formlet[List[(String, Int)], Map[String, EnvValue], Err] {
      val validate = f
      override def html = NodeSeq.Empty;
    }

  def form(implicit lang: Language) = {
    val order = ((Order.apply _).curried)(("" /: Range.apply(0, 5))((acc, v) => acc + random.nextInt(9)))
    val ? = Loc.loc0(lang) _

    Formlet(order) <*>
      inputText("lname")(validName("lname", "invalid.name", ?("order.last.name").text)) <*>
      inputText("fname")(validName("fname", "invalid.name", ?("order.first.name").text)) <*>
      inputText("region")(validName("region", "invalid.name", ?("order.region").text)) <*>
      inputText("city")(validName("city", "invalid.name", ?("order.city").text)) <*>
      inputText("address")(validName("address", "invalid.name", ?("order.address").text)) <*>
      inputText("email")(validEmail) <*>
      inputText("phone")(validPhone) <*>
      inputItems("items")(validItems)
  }
}

