package net.shop
package web.services

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates
import net.shift.html._
import net.shift.js._
import net.shift.loc.Language
import net.shop.messaging._
import net.shop.web.ShopApplication
import net.shift.engine.http.JsResponse
import net.shift.loc.Loc
import net.shop.web.pages.OrderPage
import net.shop.web.pages.OrderState
import net.shop.api.Company
import net.shop.api.Person

object OrderService extends HttpPredicates with FormValidation {

  private def normalizeParams(lang: Language, params: Map[String, String]): Try[Map[String, OrderForm.type#EnvValue]] = {
    import OrderForm._
    val init: Try[Map[String, OrderForm.type#EnvValue]] = Success(Map.empty[String, OrderForm.type#EnvValue])
    (init /: params) {
      case (Failure(t), _) => Failure(t)
      case (Success(acc), (k, v)) if (k startsWith "item") =>
        val dk = k drop 5

        ShopApplication.persistence.productById(dk) map { prod =>
          (acc get "items") match {
            case Some(OrderItems(l)) => acc + ("items" -> OrderItems(l ++ List((prod, v.toInt))))
            case _                   => acc + ("items" -> OrderItems(List((prod, v.toInt))))
          }

        }
      case (Success(acc), (k, v)) =>
        Success(acc + (k -> FormField(v)))
    }
  }

  def order = {
    for {
      r <- req
      Path("order" :: Nil) <- path
    } yield service(resp => {
      val params = r.params.map { case (k, v) => (k, v.head) }

      import JsDsl._

      normalizeParams(r.language, params) match {
        case Success(norm) =>

          val v = if (norm.contains("cif")) OrderForm.companyForm(r.language) else OrderForm.form(r.language)

          (v validate norm) match {
            case Valid(o) =>
              Future {
                resp(JsResponse(
                  func() {
                    JsStatement(
                      apply("cart.hideCart"),
                      apply("window.cart.clear"),
                      $(s"#notice_i") ~
                        apply("text", Loc.loc0(r.language)("order.done").text) ~
                        apply("show") ~
                        apply("delay", "5000") ~
                        apply("fadeOut", "slow"))
                  }.wrap.apply.toJsString))
              }
              Future {
                (o.submitter match {
                  case c: Company =>
                    OrderPage.orderCompanyTemplate(OrderState(o.toOrderLog, r.language))
                  case c: Person =>
                    OrderPage.orderTemplate(OrderState(o.toOrderLog, r.language))
                }) map { n => Messaging.send(OrderDocument(r.language, o, n toString)) }

              }
            case Invalid(msgs) =>
              respValidationFail(resp, msgs)
          }

        case Failure(t) =>
          resp(JsResponse(
            func() {
              JsStatement(
                apply("cart.hideCart"),
                $(s"#notice_e") ~
                  apply("text", Loc.loc0(r.language)("order.fail").text) ~
                  apply("show") ~
                  apply("delay", "5000") ~
                  apply("fadeOut", "slow"))
            }.wrap.apply.toJsString))
      }
    })
  }
}
