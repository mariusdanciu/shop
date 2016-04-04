package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies
import net.shop.web.pages.Cart
import net.shift.engine.http.Request
import net.shift.loc.Loc
import net.shop.api.ProductDetail
import net.shop.web.pages.ProductsQuery
import scala.util.Failure
import scala.util.Success
import net.shop.utils.ShopUtils._
import scala.xml.NodeSeq
import net.shift.template.SnipState
import net.shift.template.HasClass
import net.shift.common.Xml
import net.shift.template.Binds.bind
import net.shift.template.HasId
import net.shift.common.XmlImplicits._

trait ProductsPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List(products) ++ super.snippets

  val products = reqSnip("products") {
    s =>
      {
        val prods = ProductsQuery.fetch(s.state.initialState, store) match {
          case Success(list) =>

            val (nopos, pos) = list.span(p => p.position.isEmpty)

            (pos flatMap { (p: ProductDetail) => render(s, p) }) ++
              (nopos flatMap { (p: ProductDetail) => render(s, p) })

          case Failure(t) => errorTag(Loc.loc0(s.state.lang)("no.category").text)
        }
        Success((s.state.initialState, prods.toSeq))
      }
  }

  private def render(s: SnipState[Request], prod: ProductDetail): NodeSeq = {
    bind(s.node) {
      case Xml(name, HasId("prod_pic", a), childs) =>
        <div id={ prod stringId } title={ prod title_? (s.state.lang.name) } style={ "background: url('" + imagePath("thumb", prod) + "') no-repeat" }>{ childs }</div> % a
      case Xml(name, HasId("prod_title", a), childs) =>
        <div>{ prod title_? (s.state.lang.name) }</div> % a
      case Xml(name, HasId("prod_price", a), childs) =>
        <div>{ price(prod.price) } Lei</div> % a
    } match {
      case Success(n) => n
      case Failure(f) => errorTag(f toString)
    }
  }
}