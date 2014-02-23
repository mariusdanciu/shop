package net.shop
package web.services

import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates
import net.shop.web.form.OrderForm
import net.shift.js._
import net.shift.html._
import net.shift.engine.http.JsResponse
import net.shift.loc.Loc
import net.shop.backend.OrderSubmitter
import net.shop.orders.StoreObserver
import scala.concurrent._
import ExecutionContext.Implicits.global
import net.shop.web.pages.OrderPage
import net.shop.web.pages.OrderState
import backend._
import net.shop.orders.MailObserver

object OrderService extends HttpPredicates {

  OrderSubmitter accept StoreObserver
  OrderSubmitter accept MailObserver

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
            println(v)
            
            v map { n => OrderSubmitter.placeOrder(OrderDocument(r.language, o, n toString)) }
          }
        case Failure(msgs) => {

          val js = func() {
            JsStatement(
              (for {
                m <- msgs
              } yield {
                $(s"label[for='${m._1}']") ~
                  apply("css", "color", "#ff0000") ~
                  apply("attr", "title", m._2)
              }): _*)
          }.wrap.apply
          resp(JsResponse(js.toJsString))
        }
      }
    })
  }
}