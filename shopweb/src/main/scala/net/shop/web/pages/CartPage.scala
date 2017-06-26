package net.shop.web.pages

import net.shift.common.{Base64, Xml}
import net.shift.common.XmlImplicits._
import net.shift.loc.Loc
import net.shift.server.http.Request
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.{HasClass, HasName}
import net.shop.api.{Address, Cart, CartItem, ProductDetail}
import net.shop.utils.{ShopUtils, ThumbPic}
import net.shop.utils.ShopUtils._
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, string2JsonInput}

import scala.util.Success
import scala.xml.{NodeSeq, Text}

case class CartInfo(r: Request, items: Seq[(String, Int, ProductDetail)])

trait CartPage extends PageCommon[CartInfo] {
  self =>

  implicit val formats = DefaultFormats

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

      val emptyMsg = Loc.loc0(s.state.lang)("cart.empty").text
      Success((CartInfo(req, info), ci.map { c => if (c.items.isEmpty) emptyMsg else "" } getOrElse emptyMsg))
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
      val res = for {(id, count, prod) <- c.items} yield {
        bind(s.node) {
          case Xml("a", HasClass("hover", a), childs) =>
            Xml("a", a + ("href", ShopUtils.productPage(prod.stringId)), childs)
          case Xml("img", a, _) => Xml("img", a +
            ("src", ShopUtils.imagePath(ThumbPic, prod.stringId)) +
            ("alt", prod.title_?(s.state.lang.name) + ShopUtils.OBJECT_SUFFIX))
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
      val nodes = for {i <- 1 to 50} yield {
        <option value={i.toString}>
          {i}
        </option>
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
                case Nil => Address(None, "", "", "", "", "", "")
                case h :: _ => h
              }

              bind(s.node) {
                case HasName("fname", a) => Xml("input", a + ("value", ud.userInfo.firstName))
                case HasName("lname", a) => Xml("input", a + ("value", ud.userInfo.lastName))
                case HasName("email", a) => Xml("input", a + ("value", user.name))
                case HasName("phone", a) => Xml("input", a + ("value", ud.userInfo.phone))
                case HasName("cnp", a) => Xml("input", a + ("value", ud.userInfo.cnp))
                case HasName("region", a) => Xml("input", a + ("value", addr.region))
                case HasName("city", a) => Xml("input", a + ("value", addr.city))
                case HasName("address", a) => Xml("input", a + ("value", addr.address))
                case HasName("zip", a) => Xml("input", a + ("value", addr.zipCode))
              } map {
                (s.state.initialState, _)
              }
            case _ => Success((s.state.initialState, s.node))
          }
        case _ => Success((s.state.initialState, s.node))
      }

  }

  override def inlines = List(emptyMsg, total) ++ super.inlines

  override def snippets = List(cartProds, quantities, empty, userInfo) ++ super.snippets

  private def getCart(r: Request): Option[Cart] = {
    for {
      json <- r.uri.param("cart")
    } yield {
      val enc = java.net.URLDecoder.decode(json.value.head, "UTF-8")
      val dec = Base64.decodeString(enc)
      val cart = parse(dec).extract[net.shop.api.Cart]
      cart
    }
  }
}

object SSS extends App {
  implicit val formats = DefaultFormats
  import org.json4s.native.Serialization._

  val json = """[{"id":"55fce5a3e4b0aa0fae6d3553","count":1}]"""

  println(parse(json).extract[net.shop.api.Cart])

  val str = write(Cart(List(CartItem("55fce5a3e4b0aa0fae6d3553", 1))))

  println(str)
}