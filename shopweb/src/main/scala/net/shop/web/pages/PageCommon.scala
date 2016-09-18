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
import net.shop.web.services.ServiceDependencies
import IODefaults._
import net.shift.io.LocalFileSystem
import net.shift.security.Permission

trait PageCommon[T] extends DynamicContent[T] {

  override def inlines = List(authClass, logout)
  def snippets = List(connectError, user, permissions)

  def reqSnip(name: String) = snip[T](name) _

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

  val authClass = inline[T]("auth_class") {
    s =>
      val icon = if (s.state.user.isEmpty)
        "icon-login"
      else
        "icon-exit"
      Success((s.state.initialState, icon))
  }

  val logout = inline[T]("logout") {
    s =>
      val icon = if (s.state.user.isEmpty)
        "#"
      else
        "/logout"
      Success((s.state.initialState, icon))
  }

  val permissions = snip[T]("permissions") {
    s =>

      val perms = s.params.map(Permission(_))
      s.state.user match {
        case Some(u) => u.requireAll(perms: _*)((s.state.initialState, s.node)).recover { case t => (s.state.initialState, NodeSeq.Empty) }
        case _       => Success((s.state.initialState, NodeSeq.Empty))
      }

  }
}