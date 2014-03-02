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
import utils.ShopUtils._
import net.shift.common.XmlUtils
import net.shop.model.ProductDetail

object CartItemNode extends DynamicContent[CartState] with XmlUtils {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      bind(s.node) {
        case "img" > (a / _) if a.hasClass("thumb") => <img/> % a attr ("src", imagePath("thumb", s.state.prod)) e
        case "span" > (a / _) if a.hasClass("cart_title") => <span>{ s.state.prod.title_?(s.language) }</span> % a
        case "input" > (a / _) => (<input/> % a attr ("value", s.state.quantity toString)) attr ("onkeyup", s"cart.setItemCount('${s.state.prod.id}', this.value)") e
        case "img" > (a / _) => <img/> % a attr ("onClick", "cart.removeItem(" + s.state.prod.id + ")") e
        case "span" > (a / _) if a.hasClass("cart_price") => <span>{ s.state.prod.price }</span> % a
      } map ((s.state, _))
  }

}

case class CartState(quantity: Int, prod: ProductDetail)

