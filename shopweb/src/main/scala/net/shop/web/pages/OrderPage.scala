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

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors {

  override def snippets = List(info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  def orderTemplate(state: OrderState): Try[NodeSeq] = for {
    input <- state.req.resource(Path("web/templates/order.html"))
    template <- load(input)
  } yield new Html5(state, state.req.language, this)(bySnippetAttr[SnipState[OrderState]]).resolve(template)

  val info = reqSnip("info") {
    s =>
      val n = (for {
        id <- makeField(s.node, "order.id", s.state.o.id)
        ln <- makeField(s.node, "order.last.name", s.state.o.lastName)
        fn <- makeField(s.node, "order.first.name", s.state.o.firstName)
        region <- makeField(s.node, "order.region", s.state.o.region)
        city <- makeField(s.node, "order.city", s.state.o.city)
        address <- makeField(s.node, "order.address", s.state.o.address)
        email <- makeField(s.node, "order.email", s.state.o.email)
        phone <- makeField(s.node, "order.phone", s.state.o.phone)
      } yield { id ++ ln ++ fn ++ region ++ city ++ address ++ email ++ phone }) match {
        case Success(n) => n
        case Failure(f) => errorTag(f toString)
      }

      Success((s.state, n))
  }

  def makeField(node: NodeSeq, id: String, value: String) = {
    bind(node) {
      case n > ((a @ Attrs(("id", "field"))) / _) => <span data-loc={ id }></span> % a
      case n > ((a @ Attrs(("id", "value"))) / _) => <span>{ value }</span> % a
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
                  case "img" > (a / _) => <img/> % a attr ("src", "http://localhost:8080" + ShopUtils.productImagePath(prod)) e
                  case "td" > (a / _) if (a hasClass "c1") => <td>{ prod.title }</td> % a
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

