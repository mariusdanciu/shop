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

trait CartPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List(cartProds) ++ super.snippets

  val cartProds = reqSnip("cart_prods") {
    s =>
      implicit val formats = DefaultFormats
      val req = s.state.initialState
      val res = for { json <- req.cookie("cart") } yield {
        val cart = parse(java.net.URLDecoder.decode(json.value, "UTF-8")).extract[net.shop.api.Cart]
        val nodes = for { item <- cart.items } yield {

          (store.productById(item.id) match {
            case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
            case Failure(t)                 => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
            case Success(prod) =>
              bind(s.node) {
                case Xml("img", a, _) => Xml("img", a + ("src", ShopUtils.imagePath(prod.stringId, "thumb", prod.images.head)))
              }
          }) getOrElse NodeSeq.Empty

        }

        NodeSeq.fromSeq(nodes.flatten)
      }

      Success((req, res getOrElse NodeSeq.Empty))
  }

}