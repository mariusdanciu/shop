package net.shop
package web.pages

import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template._
import net.shop.api.{CartItem, ProductDetail}
import net.shop.utils.ShopUtils._
import net.shop.utils.ThumbPic
import net.shop.web.services.ServiceDependencies

trait CartItemNode extends DynamicContent[CartState] with ServiceDependencies {

  def snippets = List(item)

  def reqSnip(name: String) = snip[CartState](name) _

  val item = reqSnip("item") {
    s =>
      bind(s.node) {
        case HasClass("thumb", a) =>
          <a href={ s"/product?pid=${s.state.initialState.prod.stringId}" }>{
            Xml("img", ((a - "class") + ("src", imagePath(ThumbPic, s.state.initialState.prod))))
          }</a>
        case HasClass("cart_title", a) => <span>{ s.state.initialState.prod.title_?(s.state.lang.name) }</span> % a
        case Xml("input", a, _)        => (<input/> % (a + ("value", s.state.initialState.item.count toString)))
        case Xml("a", a, childs)       => <a id={ "del_" + s.state.initialState.index } href="#">{ childs }</a> % a
        case Xml("img", a, _)          => <img/> % a
        case Xml("span", a, _) if a.hasClass("cart_price") => <span>{
          s.state.initialState.prod.discountPrice.map { price } getOrElse price(s.state.initialState.prod.price)
        }</span> % a
      } map ((s.state.initialState, _))
  }

}

case class CartState(index: Int, item: CartItem, prod: ProductDetail)

