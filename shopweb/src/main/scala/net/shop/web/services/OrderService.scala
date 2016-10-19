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
import net.shift.engine.http.HttpPredicates
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
import net.shift.common.Valid
import net.shift.common.Invalid
import net.shift.engine.http.HttpPredicates
import net.shift.io.LocalFileSystem
import net.shift.engine.http.HttpPredicates._
import net.shift.server.http.Param
import net.shift.server.http.Responses
import net.shift.server.http.ContentType

trait OrderService extends FormValidation with TraversingSpec with ServiceDependencies { self =>

  private def extractOrder(json: String) = {
    def extractItems(items: List[JValue]): (String, OrderForm.EnvValue) = listTraverse.sequence(for {
      JObject(o) <- items
    } yield {
      o match {
        case ("name", JString(id)) :: ("value", JInt(count)) :: Nil =>
          store.productById(id) map { prod => (prod, count toInt) }
      }
    }).map(m => ("items", OrderForm.OrderItems(m))) getOrElse (("items", OrderForm.NotFound))

    val list = for {
      JObject(child) <- parse(json)
      JField(k, value) <- child if (k != "name" && k != "value")
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
      r <- post
      Path(_, _ :: "order" :: Nil) <- path
    } yield service(resp => {
      val json = IO.producerToString(r.body)

      val norms = json.map { extractOrder }

      import JsDsl._

      norms match {
        case Success(norm) =>

          val v = OrderForm.form(r.language)

          (v validate norm) match {
            case Valid(o) =>
              Future {
                resp(Responses.ok.withJsonBody(s"""{"msg" : "${Loc.loc0(r.language)("order.done").text}"}"""))
              }
              Future {
                val order = new OrderPage {
                  val cfg = self.cfg
                  val store = self.store
                }
                order.orderTemplate(OrderState(o.toOrderLog, r.language)) map { n =>
                  Messaging.send(OrderDocument(r.language, o, n toString))
                }
              }
            case Invalid(msgs) =>
              implicit val l = r.language
              respValidationFail(resp, msgs)
          }

        case Failure(t) =>
          resp(Responses.ok.withJsonBody(s"""{"msg" : "${Loc.loc0(r.language)("order.fail").text}"}"""))
      }
    })
  }

  def orderByEmail = for {
    r <- get
    Path(_, _ :: "orders" :: Nil) <- path
    email <- param("email")
  } yield service(resp => {

    import model.Formatters._

    implicit val l = r.language
    store.ordersByEmail(email) match {
      case Success(orders) =>
        resp(Responses.ok.withJsonBody(Formatter.format(orders.toList)))
      case Failure(t) =>
        resp(Responses.ok.withJsonBody(Formatter.format(ShopError(Loc.loc(r.language)("orders.not.found.for.email", List(email)).text, t))))
    }

  })

  def orderByProduct = for {
    r <- get
    Path(_, _ :: "orders" :: Nil) <- path
    id <- param("productid")
  } yield service(resp => {
    import model.Formatters._
    implicit val l = r.language
    store.ordersByProduct(id) match {
      case Success(orders) =>
        resp(Responses.ok.withJsonBody(Formatter.format(orders.toList)))
      case Failure(t: ShopError) =>
        resp(Responses.serverError.withJsonBody(Formatter.format(t)))
      case Failure(t) =>
        resp(Responses.serverError.withJsonBody(Formatter.format(ShopError(t.getMessage, t))))
    }

  })

}
