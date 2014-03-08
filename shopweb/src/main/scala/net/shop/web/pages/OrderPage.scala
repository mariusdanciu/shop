package net.shop.web.pages

import scala.util.Try
import scala.xml.{ NodeSeq, Text }
import net.shift.common.Bind
import net.shift.common.Bind
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
import ShopUtils._
import Binds._
import net.shift.common.Config

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors {

  override def snippets = List(info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  def orderTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.req.language, Path(s"web/templates/order_${state.req.language.language}.html"), this).map(in => in._2)

  val info = reqSnip("info") {
    s =>
      bind(s.node) {
        case n > ((a @ Attrs(("id", "oid"))) / _) => <span>{ s.state.o.id }</span> % a
        case n > ((a @ Attrs(("id", "lname"))) / _) => <span>{ s.state.o.lastName }</span> % a
        case n > ((a @ Attrs(("id", "fname"))) / _) => <span>{ s.state.o.firstName }</span> % a
        case n > ((a @ Attrs(("id", "region"))) / _) => <span>{ s.state.o.region }</span> % a
        case n > ((a @ Attrs(("id", "city"))) / _) => <span>{ s.state.o.city }</span> % a
        case n > ((a @ Attrs(("id", "address"))) / _) => <span>{ s.state.o.address }</span> % a
        case n > ((a @ Attrs(("id", "email"))) / _) => <span>{ s.state.o.email }</span> % a
        case n > ((a @ Attrs(("id", "phone"))) / _) => <span>{ s.state.o.phone }</span> % a
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
            ShopApplication.productsService.productById(id) match {
              case Success(prod) =>
                (bind(s.node) {
                  case "img" > (a / _) => <img/> % a attr ("src", s"http://${Config.string("host")}:${Config.string("port")}${ShopUtils.imagePath(prod)}") e
                  case "td" > (a / _) if (a hasClass "c1") => <td>{ prod.title_?(s.language) }</td> % a
                  case "td" > (a / _) if (a hasClass "c2") => <td>{ count }</td> % a
                  case "td" > (a / _) if (a hasClass "c3") => <td>{ prod.price }</td> % a
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

