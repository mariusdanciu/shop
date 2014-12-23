package net.shop
package web.pages

import scala.collection.immutable.Map
import scala.xml._
import net.shift._
import net.shift._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Snippet._
import net.shift.common.XmlUtils
import net.shop.api.ProductDetail
import net.shop.utils.ShopUtils

object CartItemNode extends DynamicContent[CartState] with XmlUtils with ShopUtils {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      bind(s.node) {
        case HasClass("thumb", a) => <img/> % a attr ("src", imagePath("thumb", s.state.initialState.prod)) e
        case HasClass("cart_title", a) => <span>{ s.state.initialState.prod.title_?(s.state.lang.language) }</span> % a
        case "input" attributes a / _ => (<input id={ "q_" + s.state.initialState.prod.stringId }/> % a attr ("value", s.state.initialState.quantity toString)) e
        case "a" attributes a / childs => <a id={ "del_" + s.state.initialState.prod.stringId } href="#">{ childs }</a>
        case "img" attributes a / _ => <img/> % a e
        case "span" attributes a / _ if a.hasClass("cart_price") => <span>{ s.state.initialState.prod.price }</span> % a
      } map ((s.state.initialState, _))
  }

}

case class CartState(quantity: Int, prod: ProductDetail)

