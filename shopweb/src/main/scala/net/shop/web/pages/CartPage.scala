package net.shop.web.pages

import net.shift.engine.http.Request
import net.shop.web.services.ServiceDependencies
import net.shop.web.services.OrderForm
import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JInt
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JString
import org.json4s.JsonAST.JValue
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import org.json4s.DefaultFormats
import net.shift.template.Binds._
import net.shift.common.XmlUtils._
import net.shift.common.Xml
import net.shift.loc.Loc
import net.shift.common.ShiftFailure
import net.shop.api.ShopError
import scala.util.Failure
import scala.util.Success
import net.shift.engine.utils.ShiftUtils
import net.shop.utils.ShopUtils
import scala.xml.NodeSeq
import net.shift.template.HasClass
import net.shift.common.XmlImplicits._
import scala.xml.Text

trait CartPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List(cartProds, cantities, empty) ++ super.snippets

  val empty = reqSnip("empty") {
    s =>
      implicit val formats = DefaultFormats
      val req = s.state.initialState
      val cart = for { json <- req.cookie("cart") } yield {
        parse(java.net.URLDecoder.decode(json.value, "UTF-8")).extract[net.shop.api.Cart]
      }
      val same = Success(s.state.initialState, s.node)
      (cart map {
        c =>
          if (c.items.size == 0)
            Success(s.state.initialState, Text(Loc.loc0(req.language)("cart.empty").text))
          else same
      }) getOrElse same
  }

  val cartProds = reqSnip("cart_prods") {
    s =>
      implicit val formats = DefaultFormats
      val req = s.state.initialState
      val res = for { json <- req.cookie("cart") } yield {
        val cart = parse(java.net.URLDecoder.decode(json.value, "UTF-8")).extract[net.shop.api.Cart]

        val nodes =
          for { item <- cart.items } yield {

            (store.productById(item.id) match {
              case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
              case Failure(t)                 => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
              case Success(prod) =>
                bind(s.node) {
                  case Xml("img", a, _) => Xml("img", a + ("src", ShopUtils.imagePath(prod.stringId, "thumb", prod.images.head)))
                  case Xml(name, HasClass("prod_desc", a), childs) =>
                    Xml(name, a) / Text(prod title_? (s.state.lang.name))
                  case Xml(name, HasClass("prod_price", a), childs) =>
                    Xml(name, a) / priceTag(prod)
                  case Xml("input", a, childs) =>
                    Xml("input", a + ("value", item.count.toString))
                }
            }) getOrElse NodeSeq.Empty

          }
        NodeSeq.fromSeq(nodes.flatten)
      }

      Success((req, res getOrElse NodeSeq.Empty))
  }

  val cantities = reqSnip("cantities") {
    s =>
      val nodes = for { i <- 1 to 50 } yield {
        <option value={ i.toString }>{ i }</option>
      }
      Success((s.state.initialState, nodes))
  }

}