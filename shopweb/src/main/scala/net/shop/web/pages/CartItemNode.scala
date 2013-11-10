package net.shop
package web.pages

import scala.collection.immutable.Map
import scala.xml._

import net.shift._
import net.shift._
import net.shift.common.XmlUtils._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Snippet._
import net.shop.backend.ProductDetail
import utils.ShopUtils._

object CartItemNode extends DynamicContent[CartState] {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      (s.state, bind(s.node) {
        case "img" > (a / _) if a.hasClass("thumb")=> <img /> % a attr("src", productImagePath(s.state.prod)) e
        case "span" > (a / _) if a.hasClass("cart_title") => <span>{s.state.prod.title}</span> % a
        case "input" > (a / _) => <input/> % a attr("value", s.state.quantity toString) e
        case "img" > (a / _) => <img /> % a attr("onClick", "cart.removeItem(" + s.state.prod.id + ")") e
        case "span" > (a / _) if a.hasClass("cart_price") => <span>{s.state.prod.price}</span> % a
      })
  }

}

case class CartState(quantity: Int, prod: ProductDetail)

