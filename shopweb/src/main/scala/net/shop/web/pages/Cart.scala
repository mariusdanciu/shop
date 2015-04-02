package net.shop
package web.pages

import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text
import net.shift._
import net.shift._
import net.shift.common.NodeOps._
import net.shift.common.XmlUtils._
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.api.ProductDetail
import net.shop.web.services.OrderForm
import net.shift.io.IODefaults
import net.shift.loc.Language

trait Cart[T] extends DynamicContent[T] with Selectors with IODefaults {

  def snippets = List(title, order, connectError, user)

  def reqSnip(name: String) = snip[T](name) _

  implicit def snipsSelector[T] = bySnippetAttr[T]

  def pageTitle(state: PageState[T]): String
  
  val title = reqSnip("title") {
    s => Success((s.state.initialState, Text(pageTitle(s.state))))
  }

  val order = snip[T]("order") {
    s =>
      bind(s.node) {
        case "form" attributes _ => <form id="order_form">{ OrderForm.form(s.state.lang).html }</form>
      } map ((s.state.initialState, _))
  }
  def price(p: Double) = if ((p % 1) == 0) "%.0f" format p else "%.2f" format p

  def priceTag(p: ProductDetail): Elem = {

    p.discountPrice match {
      case Some(discount) => <span>{ <span>{ price(discount) }</span> ++ <strike>{ price(p.price) }</strike> <span>RON</span> }</span>
      case _              => <span>{ s"${price(p.price)} RON" }</span>
    }
  }
  val connectError = snip[T]("connect_error") {
    s => Success((s.state.initialState, <div id="notice_connect_e">{ Loc.loc0(s.state.lang)("connect.fail").text }</div>))
  }

  val user = snip[T]("user") {
    s =>
      bind(s.node) {
        case name attributes Attributes(attrs) / _ =>
          node(name, attrs) / (s.state.user.map(u => Text(u.name)).getOrElse(NodeSeq.Empty))
      } map ((s.state.initialState, _))
  }
}