package net.shop
package web.services

import net.shift.common._
import net.shift.engine.Attempt
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.io.IO
import net.shift.loc.Loc
import net.shift.server.http.{Request, Responses}
import net.shop.api.{Formatter, ShopError}
import net.shop.messaging.{Messaging, OrderDocument}
import net.shop.web.pages.{OrderPage, OrderState}
import org.apache.log4j.Logger
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait OrderService extends FormValidation with TraversingSpec with ServiceDependencies {
  self =>

  private val log = Logger.getLogger(classOf[OrderService])

  private def extractOrder(json: String): Try[Map[String, OrderForm.EnvValue]] = {
    def extractItems(items: List[JValue]): Try[(String, OrderForm.OrderItems)] = listTraverse.sequence(for {
      JObject(o) <- items
    } yield {
      o match {
        case ("name", JString(id)) :: ("value", JInt(count)) :: Nil =>
          store.productById(id) map { prod => (prod, count toInt) }
        case _ => ShiftFailure("Cannot extract parameter").toTry
      }
    }).map(m => ("items", OrderForm.OrderItems(m)))

    val list = listTraverse.sequence(for {
      JObject(child) <- parse(json)
      JField(k, value) <- child if k != "name" && k != "value"
    } yield {
      value match {
        case JString(str) => Success((k, OrderForm.FormField(str)))
        case JArray(items) => extractItems(items)
        case JInt(v) => Success((k, OrderForm.FormField(v toString)))
        case e => ShiftFailure(s"Cannot extract $e").toTry
      }
    })

    list map {
      _ toMap
    }
  }


  def order: State[Request, Attempt] = {
    for {
      r <- post
      Path(_, _ :: "order" :: Nil) <- path
    } yield service(resp => {
      val json = IO.producerToString(r.body)

      val norms = json flatMap {
        extractOrder
      }

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

  def orderById: State[Request, Attempt] = for {
    r <- get
    Path(_, _ :: "orders" :: Nil) <- path
    email <- param("email")
  } yield service(resp => {

    import model.Formatters._

    implicit val l = r.language
    store.ordersById(email) map {
      orders =>
        resp(Responses.ok.withJsonBody(Formatter.format(orders.toList)))
    } recover {
      case t =>
        resp(Responses.ok.withJsonBody(Formatter.format(ShopError(Loc.loc(r.language)("orders.not.found.for.email", List(email)).text, t))))
    }

  })

  def orderByProduct: State[Request, Attempt] = for {
    r <- get
    Path(_, _ :: "orders" :: Nil) <- path
    id <- param("productid")
  } yield service(resp => {
    import model.Formatters._
    implicit val l = r.language
    store.ordersByProduct(id) map {
      orders =>
        resp(Responses.ok.withJsonBody(Formatter.format(orders.toList)))
    } recover {
      case t: ShopError =>
        resp(Responses.serverError.withJsonBody(Formatter.format(t)))
      case t =>
        resp(Responses.serverError.withJsonBody(Formatter.format(ShopError(t.getMessage, t))))
    }

  })

}
