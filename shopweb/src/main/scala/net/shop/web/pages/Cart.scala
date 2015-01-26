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
import net.shift.common.XmlUtils
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.api.ProductDetail
import net.shop.web.services.OrderForm

trait Cart[T] extends DynamicContent[T] with XmlUtils with Selectors {

  def snippets = List(order, connectError, user)

  def reqSnip(name: String) = snip[T](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  val order = snip[T]("order") {
    s =>
      bind(s.node) {
        case "form" attributes _ => <form id="order_form">{ OrderForm.form(s.state.lang).html }</form>
      } map ((s.state.initialState, _))
  }

  def priceTag(p: ProductDetail): Elem = p.discountPrice match {
    case Some(discount) => <span>{ <span>{ discount }</span> ++ <strike>{ p.price }</strike> <span>RON</span> }</span>
    case _              => <span>{ s"${p.price} RON" }</span>
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