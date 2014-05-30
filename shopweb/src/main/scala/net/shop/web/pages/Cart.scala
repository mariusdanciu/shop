package net.shop
package web.pages

import net.shift._
import net.shift._
import net.shift.common.Path
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import scala.xml.NodeSeq
import scala.util.Try
import net.shift.common.XmlUtils
import net.shop.web.form.OrderForm
import scala.util.Success
import scala.util.Failure
import net.shop.model.ProductDetail
import scala.xml.{ Elem, Text }
import scala.xml.Group

trait Cart[T] extends DynamicContent[T] with XmlUtils with Selectors {

  def snippets = List(order)

  def reqSnip(name: String) = snip[T](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  val order = snip[T]("order") {
    s =>
      bind(s.node) {
        case "form" :/ _ => <form id="order_form">{ OrderForm.form(s.language).html }</form>
      } map ((s.state, _))
  }

  def priceTag(p: ProductDetail): Elem = p.oldPrice match {
    case Some(old) => <span>{<span>{ p.price }</span> ++ <strike>{ old }</strike> <span>RON</span>}</span>
    case _ => <span>{ s"${p.price} RON" }</span>
  }
}