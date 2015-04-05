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
import net.shift.common.XmlUtils._
import net.shop.api.ProductDetail
import net.shop.utils.ShopUtils._
import net.shift.common.NodeOps._
import net.shop.api.CartItem

object CartItemNode extends DynamicContent[CartState] {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      bind(s.node) {
        case HasClass("thumb", a) =>
          val title = ("" /: s.state.initialState.item.userOptions){(a, e) => a + e._1 + "  :  " + e._2 + "\n"}
          node("img", (a.attrs + ("src" -> imagePath("thumb", s.state.initialState.prod)) + ("title" -> title)))
        case HasClass("cart_title", a) => <span>{ s.state.initialState.prod.title_?(s.state.lang.name) }</span> % a
        case "input" attributes a / _  => (<input id={ "q_" + s.state.initialState.prod.stringId }/> % a attr ("value", s.state.initialState.item.count toString)) e
        case "a" attributes a / childs => <a id={ "del_" + s.state.initialState.prod.stringId } href="#">{ childs }</a> % a
        case "img" attributes a / _    => <img/> % a e
        case "span" attributes a / _ if a.hasClass("cart_price") => <span>{
          s.state.initialState.prod.discountPrice.map { price } getOrElse price(s.state.initialState.prod.price)
        }</span> % a
      } map ((s.state.initialState, _))
  }

}

case class CartState(item: CartItem, prod: ProductDetail)

