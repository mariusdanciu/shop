package net.shop.web.pages

import scala.util.Try
import scala.xml.{ NodeSeq, Text }
import net.shift.common.Bind
import net.shift.common.Path
import net.shift.common.XmlUtils
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.loc.Language
import net.shift.template._
import Snippet.snip
import net.shop.model.Order
import net.shop.web.ShopApplication
import scala.util.Success
import net.shop.utils.ShopUtils
import scala.util.Failure
import Binds._
import net.shift.common.Config
import net.shop.utils.ShopUtils

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors with ShopUtils {

  override def snippets = List(info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  def orderTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.req.language, Path(s"web/templates/order_${state.req.language.language}.html"), this).map(in => in._2)

  val info = reqSnip("info") {
    s =>
      bind(s.node) {
        case n :/ HasId("oid", a) / _ => <span>{ s.state.o.id }</span> % a
        case n :/ HasId("lname", a) / _ => <span>{ s.state.o.lastName }</span> % a
        case n :/ HasId("fname", a) / _ => <span>{ s.state.o.firstName }</span> % a
        case n :/ HasId("region", a) / _ => <span>{ s.state.o.region }</span> % a
        case n :/ HasId("city", a) / _ => <span>{ s.state.o.city }</span> % a
        case n :/ HasId("address", a) / _ => <span>{ s.state.o.address }</span> % a
        case n :/ HasId("email", a) / _ => <span>{ s.state.o.email }</span> % a
        case n :/ HasId("phone", a) / _ => <span>{ s.state.o.phone }</span> % a
      } match {
        case Success(n) => Success((s.state, n))
        case Failure(f) => Success((s.state, errorTag(f toString)))
      }

  }

  val content = reqSnip("content") {
    s =>
      {
        val items: (NodeSeq, Double) = ((NodeSeq.Empty, 0.0) /: s.state.o.items) {
          case (acc, (id, count)) =>
            ShopApplication.productsService(s.language).productById(id) match {
              case Success(prod) =>
                (bind(s.node) {
                  case "img" :/ a / _ => <img/> % a attr ("src", s"http://${Config.string("host")}:${Config.string("port")}${imagePath(prod.id, "thumb", prod.images.head)}") e
                  case "td" :/ HasClass("c1", a) / _ => <td>{ prod.title_?(s.language) }</td> % a
                  case "td" :/ HasClass("c2", a) / _ => <td>{ count }</td> % a
                  case "td" :/ HasClass("c3", a) / _ => <td>{ prod.price }</td> % a
                }) match {
                  case Success(n) => (acc._1 ++ n, acc._2 + prod.price * count)
                  case Failure(f) => (acc._1 ++ errorTag(f toString), 0.0)
                }
              case Failure(f) => (acc._1 ++ errorTag(f toString), 0.0)
            }
        }

        Success((s.state.copy(total = items._2), items._1))
      }
  }

  val total = reqSnip("total") {
    s => Success((s.state, Text(s.state.total.formatted("%.2f"))))
  }
}

case class OrderState(o: Order, req: Request, total: Double)

