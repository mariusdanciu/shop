package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies
import net.shop.web.pages.Cart
import net.shift.engine.http.Request
import net.shop.api.ShopError
import net.shift.loc.Loc
import net.shift.template.SnipState
import net.shop.api.ProductDetail
import net.shop.web.pages.ProductsQuery
import net.shift.common.ShiftFailure
import scala.util.Failure
import scala.util.Try
import scala.util.Success
import net.shop.web.pages.ProductPageState
import scala.xml.Text
import net.shift.template.Binds.bind
import net.shift.template.HasId
import net.shift.common.XmlImplicits._
import net.shift.common.Xml
import net.shop.utils.ShopUtils._
import net.shop.api.ProductDetail
import net.shop.api.ShopError
import net.shift.template.HasClass
import net.shift.common.XmlAttr

trait ProductPage extends MobilePage with ServiceDependencies { self =>

  override def snippets = List(item) ++ super.snippets

  def product(s: SnipState[Request]): Try[ProductDetail] = {
    s.state.initialState.param("pid") match {
      case Some(id :: _) =>
        store.productById(id) match {
          case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
          case Failure(t)                 => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
          case s                          => s
        }
      case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
    }
  }

  val item = reqSnip("item") {
    s =>
      {
        product(s) match {
          case Success(p) =>
            bind(s.node) {
              case Xml(name, HasClass("prod_title", a), childs) =>
                Xml(name, a, <h2>{ p.title_?(s.state.lang.name) }</h2>)
              case Xml(name, HasClass("prod_price", a), childs) =>
                <div>{ price(p.price) } Lei</div> % a
              case Xml(name, HasClass("prod_desc", a), childs) =>
                <div>{ p.description_?(s.state.lang.name) }</div> % a
              case Xml(name, HasClass("prod_pic", a), childs) =>
                Xml(name) % XmlAttr(a + ("style", "background: url('" + imagePath("normal", p) + "') no-repeat"))
            } map { xml =>
              (s.state.initialState, xml)
            }

          case Failure(f) => Failure(f)
        }
      }
  }
}