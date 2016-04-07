package net.shop.web.pages.mobile

import net.shift.template.DynamicContent
import net.shift.loc.Loc
import scala.xml.Elem
import net.shift.template.Selectors
import net.shop.api.ProductDetail
import net.shift.io.IODefaults
import net.shop.web.services.OrderForm
import net.shift.common.Xml
import net.shift.engine.http.Request
import scala.util.Success
import net.shift.template.Snippet._
import net.shop.utils.ShopUtils._
import net.shift.template.Binds._
import scala.xml.NodeSeq
import scala.xml.Text
import net.shift.common.XmlImplicits._

trait MobilePage extends DynamicContent[Request] with Selectors with IODefaults {

  def snippets = List(order, connectError, user, back)

  def reqSnip(name: String) = snip[Request](name) _

  implicit def snipsSelector[T] = bySnippetAttr[T]

  val back = reqSnip("back") {
    s =>
      val req = s.state.initialState
      
      if (req.path.parts == List("mobile")) {
        Success((s.state.initialState, NodeSeq.Empty))
      } else {
        Success((s.state.initialState, s.node))
      }
  }

  val order = reqSnip("order") {
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
  val connectError = reqSnip("connect_error") {
    s => Success((s.state.initialState, <div id="notice_connect_e">{ Loc.loc0(s.state.lang)("connect.fail").text }</div>))
  }

  val user = reqSnip("user") {
    s =>
      bind(s.node) {
        case Xml(name, attrs, _) =>
          Xml(name, attrs) / (s.state.user.map(u => Text(u.name)).getOrElse(NodeSeq.Empty))
      } map ((s.state.initialState, _))
  }
}