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
import net.shop.api.CartItem
import net.shift.common.BNodeImplicits._
import net.shift.common.BNode

object CartItemNode extends DynamicContent[CartState] {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      bind(s.node) {
        case HasClass("thumb", a) =>
          val title = ("" /: s.state.initialState.item.userOptions) { (a, e) => a + e._1 + " : " + e._2 + "\n" }
          <a href={ s"/product?pid=${s.state.initialState.prod.stringId}" }>{
            BNode("img", ((a - "class") + ("src", imagePath("thumb", s.state.initialState.prod)) + ("title", title))) toElem
          }</a>
        case HasClass("cart_title", a) => <span>{ s.state.initialState.prod.title_?(s.state.lang.name) }</span> % a
        case BNode("input", a, _)      => (<input/> % (a + ("value", s.state.initialState.item.count toString)))
        case BNode("a", a, childs)     => <a id={ "del_" + s.state.initialState.index } href="#">{ childs }</a> % a
        case BNode("img", a, _)        => <img/> % a
        case BNode("span", a, _) if a.hasClass("cart_price") => <span>{
          s.state.initialState.prod.discountPrice.map { price } getOrElse price(s.state.initialState.prod.price)
        }</span> % a
      } map ((s.state.initialState, _))
  }

}

case class CartState(index: Int, item: CartItem, prod: ProductDetail)

