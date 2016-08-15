package net.shop.web.pages.mobile

import net.shift.template.DynamicContent
import net.shift.loc.Loc
import scala.xml.Elem
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
import net.shift.template.HasClass

trait MobilePage extends DynamicContent[Request]  with IODefaults {

  def snippets = List(connectError, user, back)

  def reqSnip(name: String) = snip[Request](name) _

  val back = reqSnip("back") {
    s =>
      val req = s.state.initialState

      if (req.path.parts == List("mobile")) {
        Success((s.state.initialState, NodeSeq.Empty))
      } else {
        Success((s.state.initialState, s.node))
      }
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
      val content = s.state.user match {
        case Some(usr) =>
          bind(s.node) {
            case Xml(name, HasClass("user_name", attrs), _) =>
              Xml(name, attrs) / Text(usr.name)
          }
        case _ => Success(NodeSeq.Empty)
      }

      content map { ((s.state.initialState, _)) }
  }
}