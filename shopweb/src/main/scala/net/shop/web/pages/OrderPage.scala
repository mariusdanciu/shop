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
import net.shop.api.OrderLog
import net.shop.api.Person
import net.shop.api.Company

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors with ShopUtils {

  override def snippets = List(logo, info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  def orderTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.lang, Path(s"web/templates/order_${state.lang.language}.html"), this).map(in => in._2)

  def orderCompanyTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(state, state.lang, Path(s"web/templates/order_company_${state.lang.language}.html"), this).map(in => in._2)

  val logo = reqSnip("logo") {
    s => Success((s.state, <img src={ s"http://${Config.string("host")}:${Config.string("port")}/static/images/idid-small.png" }/>))
  }

  val info = reqSnip("info") {
    s =>
      s.state.o match {
        case OrderLog(id, time, Person(fn, ln), region, city, address, email, phone, _) =>
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

        case OrderLog(id, time, Company(cn, cif, regCom, bank, account), region, city, address, email, phone, _) =>
          bind(s.node) {
            case n :/ HasId("oid", a) / _ => <span>{ id }</span> % a
            case n :/ HasId("cname", a) / _ => <span>{ cn }</span> % a
            case n :/ HasId("cif", a) / _ => <span>{ cif }</span> % a
            case n :/ HasId("cregcom", a) / _ => <span>{ regCom }</span> % a
            case n :/ HasId("cbank", a) / _ => <span>{ bank }</span> % a
            case n :/ HasId("cbankaccount", a) / _ => <span>{ account }</span> % a
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
        val items = (NodeSeq.Empty /: s.state.o.items) {
          case (acc, prod) =>
            ShopApplication.persistence.productById(prod.id) match {
              case Success(p) =>
                (bind(s.node) {
                  case "img" :/ a / _ =>
                    <img/> % a attr ("src", s"http://${Config.string("host")}:${Config.string("port")}${imagePath(prod.id, "thumb", p.images.head)}") e
                  case "td" :/ HasClass("c1", a) / _ => <td>{ p.title_?(s.language) }</td> % a
                  case "td" :/ HasClass("c2", a) / _ => <td>{ prod.quantity }</td> % a
                  case "td" :/ HasClass("c3", a) / _ => <td>{ p.price }</td> % a
                }) match {
                  case Success(n) => acc ++ n
                  case Failure(f) => acc ++ errorTag(f toString)
                }
              case Failure(f) => errorTag(f getMessage)
            }
        }

        Success(s.state, items)
      }
  }

  val total = reqSnip("total") {
    s => Success((s.state, Text(s.state.o.total.formatted("%.2f"))))
  }
}

case class OrderState(o: OrderLog, lang: Language)

