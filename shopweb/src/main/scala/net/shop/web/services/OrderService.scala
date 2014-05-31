package net.shop
package web.services

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import backend._
import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates
import net.shift.html._
import net.shift.js._
import net.shop.backend.OrderSubmitter
import net.shop.orders.OrderListener
import net.shop.web.form.OrderForm
import net.shift.engine.http.JsResponse
import net.shift.loc.Loc
import net.shop.web.pages.OrderPage
import net.shop.web.pages.OrderState

object OrderService extends HttpPredicates {

  OrderSubmitter accept OrderListener

  private def normalizeParams(params: Map[String, String]): Map[String, OrderForm.type#EnvValue] = {
    import OrderForm._
    (Map.empty[String, OrderForm.type#EnvValue] /: params) {
      case (acc, (k, v)) => if (k startsWith "item") {
        val dk = k drop 5
        (acc get "items") match {
          case Some(OrderItems(l)) => acc + ("items" -> OrderItems(l ++ List((dk, v.toInt))))
          case _ => acc + ("items" -> OrderItems(List((dk, v.toInt))))
        }
      } else {
        acc + (k -> FormField(v))
      }
    }
  }

  def get = {
    for {
      r <- req
      Path("order" :: Nil) <- path
    } yield service(resp => {
      val params = r.params.map { case (k, v) => (k, v.head) }

      import JsDsl._
      (OrderForm.form(r.language) validate normalizeParams(params)) match {
        case Success(o) =>
          future {
            resp(JsResponse(
              apply("cart.orderDone", Loc.loc0(r.language)("order.done").text) toJsString))
          }
          future {
            val v = OrderPage.orderTemplate(OrderState(o, r, 0.0))
            v map { n => OrderSubmitter.placeOrder(OrderDocument(r.language, o, n toString)) }
          }
        case Failure(msgs) => {
          resp(JsResponse(
            func() {
              JsStatement(
                (for {
                  m <- msgs
                } yield {
                  $(s"label[for='${m._1}']") ~
                    apply("css", "color", "#ff0000") ~
                    apply("attr", "title", m._2)
                }): _*)
            }.wrap.apply.toJsString))
        }
      }
    })
  }
}