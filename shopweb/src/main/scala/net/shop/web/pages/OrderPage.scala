package net.shop.web.pages

import scala.util.Try
import scala.xml.{ NodeSeq, Text }
import net.shift.common.Bind
import net.shift.common.Path
import net.shift.common.XmlUtils
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.loc.Language
import net.shift.template._
import Snippet.snip
import net.shop.model._
import net.shop.web.ShopApplication
import scala.util.Success
import net.shop.utils.ShopUtils
import scala.util.Failure
import Binds._
import net.shift.common.Config
import net.shop.utils.ShopUtils

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors with ShopUtils {

  override def snippets = List(logo, info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  def orderTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.req.language, Path(s"web/templates/order_${state.req.language.language}.html"), this).map(in => in._2)

  def orderCompanyTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.req.language, Path(s"web/templates/order_company_${state.req.language.language}.html"), this).map(in => in._2)

  val logo = reqSnip("logo") {
    s => Success((s.state, <img src={ s"http://${Config.string("host")}:${Config.string("port")}/static/images/idid-small.png" }/>))
  }

  val info = reqSnip("info") {
    s =>
      s.state.o match {
        case Order(id, Person(fn, ln), region, city, address, email, phone, _, _) =>
          bind(s.node) {
            case n :/ HasId("oid", a) / _ => <span>{ id }</span> % a
            case n :/ HasId("lname", a) / _ => <span>{ ln }</span> % a
            case n :/ HasId("fname", a) / _ => <span>{ fn }</span> % a
            case n :/ HasId("region", a) / _ => <span>{ region }</span> % a
            case n :/ HasId("city", a) / _ => <span>{ city }</span> % a
            case n :/ HasId("address", a) / _ => <span>{ address }</span> % a
            case n :/ HasId("email", a) / _ => <span>{ email }</span> % a
            case n :/ HasId("phone", a) / _ => <span>{ phone }</span> % a
          } match {
            case Success(n) => Success((s.state, n))
            case Failure(f) => Success((s.state, errorTag(f toString)))
          }

        case Order(id, Company(cn, cif, regCom, bank, account), region, city, address, email, phone, _, _) =>
          bind(s.node) {
            case n :/ HasId("oid", a) / _ => <span>{ id }</span> % a
            case n :/ HasId("cname", a) / _ => <span>{ cn }</span> % a
            case n :/ HasId("cif", a) / _ => <span>{ cif }</span> % a
            case n :/ HasId("cregcom", a) / _ => <span>{ regCom }</span> % a
            case n :/ HasId("cbank", a) / _ => <span>{ bank }</span> % a
            case n :/ HasId("cbankaccount", a) / _ => <span>{ account}</span> % a
            case n :/ HasId("cregion", a) / _ => <span>{ region }</span> % a
            case n :/ HasId("ccity", a) / _ => <span>{ city }</span> % a
            case n :/ HasId("caddress", a) / _ => <span>{ address }</span> % a
            case n :/ HasId("cemail", a) / _ => <span>{ email }</span> % a
            case n :/ HasId("cphone", a) / _ => <span>{ phone }</span> % a
          } match {
            case Success(n) => Success((s.state, n))
            case Failure(f) => Success((s.state, errorTag(f toString)))
          }

      }

  }

  val content = reqSnip("content") {
    s =>
      {
        val items: (NodeSeq, Double) = ((NodeSeq.Empty, 0.0) /: s.state.o.items) {
          case (acc, (prod, count)) =>
            (bind(s.node) {
              case "img" :/ a / _ => <img/> % a attr ("src", s"http://${Config.string("host")}:${Config.string("port")}${imagePath(prod.id, "thumb", prod.images.head)}") e
              case "td" :/ HasClass("c1", a) / _ => <td>{ prod.title_?(s.language) }</td> % a
              case "td" :/ HasClass("c2", a) / _ => <td>{ count }</td> % a
              case "td" :/ HasClass("c3", a) / _ => <td>{ prod.price }</td> % a
            }) match {
              case Success(n) => (acc._1 ++ n, acc._2 + prod.price * count)
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

