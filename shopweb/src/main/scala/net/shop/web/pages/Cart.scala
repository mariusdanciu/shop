package net.shop
package web.pages

import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text
import net.shift._
import net.shift._
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
import net.shop.utils.ShopUtils._
import net.shift.common.Xml
import net.shift.common.XmlAttr
import net.shift.common.XmlImplicits._

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
        case Xml("form", _, _) => <form id="order_form">{ OrderForm.form(s.state.lang).html }</form>
      } map ((s.state.initialState, _))
  }


  def priceTag(p: ProductDetail): Elem = {
    p.discountPrice match {
      case Some(discount) => <span>{ <span>{ price(discount) }</span> ++ <strike>{ price(p.price) }</strike> <span>Lei</span> }</span>
      case _              => <span>{ s"${price(p.price)} Lei" }</span>
    }
  }
  val connectError = snip[T]("connect_error") {
    s => Success((s.state.initialState, <div id="notice_connect_e">{ Loc.loc0(s.state.lang)("connect.fail").text }</div>))
  }

  val user = snip[T]("user") {
    s =>
      bind(s.node) {
        case Xml(name, attrs, _) =>
          Xml(name, attrs) / (s.state.user.map(u => Text(u.name)).getOrElse(NodeSeq.Empty))
      } map ((s.state.initialState, _))
  }
}