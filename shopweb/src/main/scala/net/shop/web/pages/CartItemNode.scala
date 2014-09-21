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
        case HasClass("thumb", a) => <img/> % a attr ("src", imagePath("thumb", s.state.prod)) e
        case HasClass("cart_title", a) => <span>{ s.state.prod.title_?(s.language) }</span> % a
        case "input" :/ a / _ => (<input id={ "q_" + s.state.prod.stringId }/> % a attr ("value", s.state.quantity toString)) e
        case "a" :/ a / childs => <a id={ "del_" + s.state.prod.stringId } href="#">{ childs }</a>
        case "img" :/ a / _ => <img/> % a e
        case "span" :/ a / _ if a.hasClass("cart_price") => <span>{ s.state.prod.price }</span> % a
      } map ((s.state, _))
  }

}

case class CartState(quantity: Int, prod: ProductDetail)

