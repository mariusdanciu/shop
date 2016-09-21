package net.shop.web.pages

import scala.util.Success
import scala.xml.NodeSeq
import scala.xml.Text
import org.json4s.DefaultFormats
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import net.shift.common.XmlImplicits._
import net.shift.common.XmlUtils._
import net.shift.loc.Loc
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.api.{ Cart => CCart }
import net.shop.web.services.ServiceDependencies
import net.shop.api.ShopError
import net.shift.template.HasClass
import net.shift.common.ShiftFailure
import scala.util.Failure
import net.shift.common.Xml
import net.shop.utils.ShopUtils._
import scala.Option
import net.shop.api.ProductDetail
import net.shop.utils.ShopUtils
import net.shift.http.HTTPRequest
import net.shift.template.HasName
import net.shop.api.Address

case class CartInfo(r: HTTPRequest, items: Seq[(String, Int, ProductDetail)])

trait CartPage extends PageCommon[CartInfo] with ServiceDependencies { self =>
  implicit val formats = DefaultFormats

  override def inlines = List(emptyMsg, total) ++ super.inlines

  override def snippets = List(cartProds, quantities, empty, userInfo) ++ super.snippets

  private def getCart(r: HTTPRequest): Option[CCart] = {
    for {
      json <- r.cookie("cart")
    } yield {
      parse(java.net.URLDecoder.decode(json.cookieValue, "UTF-8")).extract[net.shop.api.Cart]
    }
  }

  val emptyMsg = inline[CartInfo]("emptyMsg") {
    s =>
      val req = s.state.initialState.r
      val ci = getCart(req)
      val info = for {
        c <- ci.toSeq
        item <- c.items
        p <- store.productById(item.id).toOption
      } yield {
        (item.id, item.count, p)
      }

      Success((CartInfo(req, info), ci.map { c => if (c.items.isEmpty) Loc.loc0(s.state.lang)("cart.empty").text else "" } getOrElse ""))
  }

  val total = inline[CartInfo]("total") {
    s =>
      val cart = s.state.initialState
      val total = cart.items.map {
        c => c._2 * c._3.price
      }.sum

      Success((cart, price(total)))
  }

  val empty = reqSnip("empty") {
    s =>
      val cart = s.state.initialState
      if (cart.items.size == 0)
        Success(s.state.initialState, NodeSeq.Empty)
      else
        Success(s.state.initialState, s.node)
  }

  val cartProds = reqSnip("cart_prods") {
    s =>
      val c = s.state.initialState
      val res = for { (id, count, prod) <- c.items } yield {
        bind(s.node) {
          case Xml("img", a, _) => Xml("img", a + ("src", ShopUtils.imagePath(prod.stringId, "thumb", prod.images.head)))
          case Xml(name, HasClass("prod_desc", a), childs) =>
            Xml(name, a) / Text(prod title_? (s.state.lang.name))
          case Xml(name, HasClass("prod_price", a), childs) =>
            Xml(name, a) / priceTag(prod)
          case Xml("option", a, childs) =>
            val value = a.attrs("value")
            if (value.toInt == count)
              Xml("option", a + ("selected", "true")) / childs
            else
              Xml("option", a) / childs
        }.getOrElse(NodeSeq.Empty)
      }
      Success((s.state.initialState, res.flatMap { n => n }))
  }

  val quantities = reqSnip("quantities") {
    s =>
      val nodes = for { i <- 1 to 50 } yield {
        <option value={ i.toString }>{ i }</option>
      }
      Success((s.state.initialState, nodes))
  }

  val userInfo = reqSnip("userInfo") {
    s =>
      s.state.user match {
        case Some(user) =>
          store.userByEmail(user.name) match {
            case scala.util.Success(Some(ud)) =>
              val addr = ud.addresses match {
                case Nil    => Address(None, "", "", "", "", "", "")
                case h :: _ => h
              }

              bind(s.node) {
                case HasName("fname", a)   => Xml("input", a + ("value", ud.userInfo.firstName))
                case HasName("lname", a)   => Xml("input", a + ("value", ud.userInfo.lastName))
                case HasName("email", a)   => Xml("input", a + ("value", user.name))
                case HasName("phone", a)   => Xml("input", a + ("value", ud.userInfo.phone))
                case HasName("cnp", a)     => Xml("input", a + ("value", ud.userInfo.cnp))
                case HasName("region", a)  => Xml("input", a + ("value", addr.region))
                case HasName("city", a)    => Xml("input", a + ("value", addr.city))
                case HasName("address", a) => Xml("input", a + ("value", addr.address))
                case HasName("zip", a)     => Xml("input", a + ("value", addr.zipCode))
              } map { (s.state.initialState, _) }
            case _ => Success((s.state.initialState, s.node))
          }
        case _ => Success((s.state.initialState, s.node))
      }

  }
}

