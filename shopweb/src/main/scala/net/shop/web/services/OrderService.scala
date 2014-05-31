package net.shop
package web.services

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import backend._
import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates
import net.shift.html._
import net.shift.js._
import net.shift.loc.Language
import net.shop.backend.OrderSubmitter
import net.shop.orders.OrderListener
import net.shop.web.ShopApplication
import net.shop.web.form.OrderForm
import net.shift.engine.http.JsResponse
import net.shift.loc.Loc
import net.shop.web.pages.OrderPage
import net.shop.web.pages.OrderState

object OrderService extends HttpPredicates {

  OrderSubmitter accept OrderListener

  private def normalizeParams(lang: Language, params: Map[String, String]): Try[Map[String, OrderForm.type#EnvValue]] = {
    import OrderForm._
    val init: Try[Map[String, OrderForm.type#EnvValue]] = Success(Map.empty[String, OrderForm.type#EnvValue])
    (init /: params) {
      case (Failure(t), _) => Failure(t)
      case (Success(acc), (k, v)) if (k startsWith "item") =>
        val dk = k drop 5

        ShopApplication.productsService(lang).productById(dk) map { prod =>
          (acc get "items") match {
            case Some(OrderItems(l)) => acc + ("items" -> OrderItems(l ++ List((prod, v.toInt))))
            case _ => acc + ("items" -> OrderItems(List((prod, v.toInt))))
          }

        }
      case (Success(acc), (k, v)) =>
        Success(acc + (k -> FormField(v)))
    }
  }

  def get = {
    for {
      r <- req
      Path("order" :: Nil) <- path
    } yield service(resp => {
      val params = r.params.map { case (k, v) => (k, v.head) }

      import JsDsl._

      normalizeParams(r.language, params) match {
        case Success(norm) => (OrderForm.form(r.language) validate norm) match {
          case net.shift.html.Success(o) =>
            future {
              resp(JsResponse(
                apply("cart.orderDone", Loc.loc0(r.language)("order.done").text) toJsString))
            }
            future {
              val v = OrderPage.orderTemplate(OrderState(o, r, 0.0))
              v map { n => OrderSubmitter.placeOrder(OrderDocument(r.language, o, n toString)) }
            }
          case net.shift.html.Failure(msgs) => {
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

        case Failure(t) =>
          resp(JsResponse(
            func() {
              JsStatement(
                apply("cart.hideCart"),
                $(s"#notice") ~
                  apply("text", Loc.loc0(r.language)("order.fail").text) ~
                  apply("show") ~
                  apply("delay", "5000") ~
                  apply("fadeOut", "slow"))
            }.wrap.apply.toJsString))
      }
    })
  }
}