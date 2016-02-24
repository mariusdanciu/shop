package net.shop
package web.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JInt
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JString
import org.json4s.JsonAST.JValue
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import OrderForm.FormField
import OrderForm.OrderItems
import net.shift.common.Path
import net.shift.common.ShiftFailure
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.GET
import net.shift.engine.http.HttpPredicates
import net.shift.engine.http.JsResponse
import net.shift.engine.http.JsonResponse
import net.shift.engine.http.POST
import net.shift.engine.http.Response.augmentResponse
import net.shift.html.Invalid
import net.shift.html.Valid
import net.shift.io.IO
import net.shift.js.JsDsl
import net.shift.js.JsStatement
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shop.api.Company
import net.shop.api.Formatter
import net.shop.api.Person
import net.shop.api.ShopError
import net.shop.messaging.Messaging
import net.shop.messaging.OrderDocument
import net.shop.model.Formatters.ErrorJsonWriter
import net.shop.model.Formatters.JsonOrdersWriter
import net.shop.web.ShopApplication
import net.shop.web.pages.OrderPage
import net.shop.web.pages.OrderState
import net.shift.common.Config

trait OrderService extends HttpPredicates with FormValidation with TraversingSpec with ServiceDependencies {self =>

  private def extractOrder(json: String) = {
    def extractItems(items: List[JValue]): (String, OrderForm.EnvValue) = listTraverse.sequence(for {
      JObject(o) <- items
    } yield {
      o match {
        case ("name", JString(id)) :: ("userOptions", l) :: ("value", JInt(count)) :: Nil =>
          store.productById(id) map { prod =>
            val opts = for {
              JObject(lo) <- l;
              (k, JString(v)) <- lo
            } yield {
              (k, v)
            }
            (prod, opts.toMap, count toInt)
          }
      }
    }).map(m => ("items", OrderForm.OrderItems(m))) getOrElse (("items", OrderForm.NotFound))

    val list = for {
      JObject(child) <- parse(json)
      JField(k, value) <- child if (k != "name" && k != "userOptions" && k != "value")
    } yield {
      value match {
        case JString(str)  => (k, OrderForm.FormField(str))
        case JArray(items) => extractItems(items)
        case JInt(v)       => (k, OrderForm.FormField(v toString))
      }
    }

    list toMap
  }

  def order = {
    for {
      r <- POST
      Path(_, "order" :: Nil) <- path
    } yield service(resp => {
      val params = r.params.map { case (k, v) => (k, v.head) }

      val json = IO.toString(r.readBody)

      val norms = json.map { extractOrder }

      import JsDsl._

      norms match {
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
                val order = new OrderPage {
                  val cfg = self.cfg
                  val store = self.store
                }
                (o.submitter match {

                  case c: Company =>
                    order.orderCompanyTemplate(OrderState(o.toOrderLog, r.language))
                  case c: Person =>
                    order.orderTemplate(OrderState(o.toOrderLog, r.language))
                }) map { n =>
                  Messaging.send(OrderDocument(r.language, o, n toString))
                }
              }
            case Invalid(msgs) =>
              implicit val l = r.language
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

  def orderByEmail = for {
    r <- GET
    Path(_, "orders" :: Nil) <- path
    email <- param("email")
  } yield service(resp => {

    import model.Formatters._

    implicit val l = r.language
    store.ordersByEmail(email) match {
      case Success(orders) =>
        resp(JsonResponse(Formatter.format(orders.toList)))
      case Failure(t) =>
        resp(JsonResponse(Formatter.format(ShopError(Loc.loc(r.language)("orders.not.found.for.email", List(email)).text, t))))
    }

  })

  def orderByProduct = for {
    r <- GET
    Path(_, "orders" :: Nil) <- path
    id <- param("productid")
  } yield service(resp => {
    import model.Formatters._
    implicit val l = r.language
    store.ordersByProduct(id) match {
      case Success(orders) =>
        resp(JsonResponse(Formatter.format(orders.toList)))
      case Failure(t: ShopError) =>
        resp(JsonResponse(Formatter.format(t)).code(500))
      case Failure(t) =>
        resp(JsonResponse(Formatter.format(ShopError(t.getMessage, t))).code(500))
    }

  })

}
