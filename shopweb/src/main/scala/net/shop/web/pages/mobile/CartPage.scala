package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies
import net.shop.web.pages.CartState
import net.shop.api.ShopError
import net.shift.template.PageState
import net.shift.engine.http.JsonResponse
import net.shift.engine.page.Html5
import org.json4s.DefaultFormats
import net.shop.web.pages.CartItemNode
import net.shift.common.Path
import scala.util.Failure
import scala.util.Success
import net.shop.api.Cart
import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import net.shop.tryApplicative
import net.shift.template.HasClasses
import net.shift.template.HasClass
import net.shift.template.SnipState
import net.shop.api.ProductDetail
import scala.xml.NodeSeq
import net.shift.common.Xml
import net.shift.engine.http.Request
import net.shift.template.Binds.bind
import net.shift.template.HasId
import net.shift.common.XmlImplicits._
import net.shift.common.XmlImplicits
import net.shift.template.HasClasses
import net.shop.utils.ShopUtils._
import net.shop.api.CartItem
import net.shift.common.XmlAttr
import net.shift.engine.http.RequestShell
import scala.xml.Text

trait CartPage extends MobilePage with ServiceDependencies { self =>

  override def snippets = List(products, total) ++ super.snippets

  val products = reqSnip("products") {
    s =>
      {
        val res = s.state.initialState.cookie("cart") match {
          case Some(c) => {
            implicit val formats = DefaultFormats
            implicit def snipsSelector[T] = bySnippetAttr[T]

            val content = for {
              (item, index) <- readCart(c.value).items.zipWithIndex
              prod <- store.productById(item.id).toOption
            } yield {
              (render(s, item, prod, index), prod.price * item.count)
            }

            (content.map { _._1 }, content.map { _._2 }.sum)
          }
          case _ => (NodeSeq.Empty, 0.0)
        }

        Success((RequestAndTotal(s.state.initialState, res._2), res._1.flatten))
      }
  }
  val total = reqSnip("total") {
    s =>
      {
        val total = s.state.initialState match {
          case RequestAndTotal(r, total) => price(total)
          case _                         => "..."
        }

        val content = bind(s.node) {
          case Xml(name, a, childs) => Xml(name, a, Text(total))
        }

        content.map { c => (s.state.initialState, c) }
      }
  }
  private def render(s: SnipState[Request], cart: CartItem, prod: ProductDetail, pos: Int): NodeSeq = {
    bind(s.node) {
      case Xml("li", a, childs) =>
        Xml("li", XmlAttr(a.attrs + ("id" -> s"del_$pos")), childs)
      case e @ Xml(_, HasClasses(_ :: "add_to_cart" :: Nil, _), _) =>
        e.addAttr("id", prod.stringId)
      case Xml("a", HasClass("prod_link", a), childs) =>
        <a href={ s"/mobile/product?pid=${prod.stringId}" }>{ childs }</a>
      case Xml(name, HasClass("prod_pic", a), childs) =>
        <div title={ prod title_? (s.state.lang.name) } style={ "background: url('" + imagePath("thumb", prod) + "') no-repeat" }>{ childs }</div> % a
      case Xml(name, HasClass("prod_title", a), childs) =>
        <div>{ prod title_? (s.state.lang.name) }</div> % a
      case Xml(name, HasClass("prod_price", a), childs) =>
        <div>{ price(prod.price) } Lei</div> % a
      case Xml("input", a, _) =>
        <input type="text" value={ cart.count.toString }></input> % a
    } match {
      case Success(n) => n
      case Failure(f) => errorTag(f toString)
    }
  }

  private def readCart(json: String): Cart = {
    implicit val formats = DefaultFormats
    parse(java.net.URLDecoder.decode(json, "UTF-8")).extract[Cart]
  }

}

case class RequestAndTotal(r: Request, total: Double) extends RequestShell(r)
