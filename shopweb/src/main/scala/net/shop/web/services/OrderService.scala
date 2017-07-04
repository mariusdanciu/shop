package net.shop
package web.services

import net.shift.common.{Invalid, Path, TraversingSpec, Valid}
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.io.IO
import net.shift.loc.Loc
import net.shift.server.http.Responses
import net.shop.api.{Formatter, ShopError}
import net.shop.messaging.{Messaging, OrderDocument}
import net.shop.web.pages.{OrderPage, OrderState}
import org.apache.log4j.Logger
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait OrderService extends FormValidation with TraversingSpec with ServiceDependencies { self =>

  private val log = Logger.getLogger(classOf[OrderService])

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

      log.debug("Order " + norms)

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
                  log.debug("Sending order message")
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
