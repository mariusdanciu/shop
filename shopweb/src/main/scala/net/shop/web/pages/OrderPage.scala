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
import net.shop.api.Address

object OrderPage extends DynamicContent[OrderState] with XmlUtils with Selectors with ShopUtils {

  override def snippets = List(logo, info, content, total)

  def reqSnip(name: String) = snip[OrderState](name) _

  implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

  def orderTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(PageState(state, state.lang), Path(s"web/templates/order_${state.lang.name}.html"), this).map(in => in._2)

  def orderCompanyTemplate(state: OrderState): Try[NodeSeq] =
    Html5.runPageFromFile(PageState(state, state.lang), Path(s"web/templates/order_company_${state.lang.name}.html"), this).map(in => in._2)

  val logo = reqSnip("logo") {
    s => Success((s.state.initialState, <img src={ s"http://${Config.string("host")}:${Config.string("port")}/static/images/logo-black-small.png" }/>))
  }

  val info = reqSnip("info") {
    s =>
      s.state.initialState.o match {
        case OrderLog(id, time, Person(fn, ln, cnp), Address(_, _, country, region, city, address, zip), email, phone, _) =>
          bind(s.node) {
            case n attributes HasId("oid", a) / _     => <span>{ id }</span> % a
            case n attributes HasId("lname", a) / _   => <span>{ ln }</span> % a
            case n attributes HasId("fname", a) / _   => <span>{ fn }</span> % a
            case n attributes HasId("cnp", a) / _     => <span>{ cnp }</span> % a
            case n attributes HasId("region", a) / _  => <span>{ region }</span> % a
            case n attributes HasId("city", a) / _    => <span>{ city }</span> % a
            case n attributes HasId("address", a) / _ => <span>{ address }</span> % a
            case n attributes HasId("email", a) / _   => <span>{ email }</span> % a
            case n attributes HasId("phone", a) / _   => <span>{ phone }</span> % a
          } match {
            case Success(n) => Success((s.state.initialState, n))
            case Failure(f) => Success((s.state.initialState, errorTag(f toString)))
          }

        case OrderLog(id, time, Company(cn, cif, regCom, bank, account), Address(_, _, country, region, city, address, zip), email, phone, _) =>
          bind(s.node) {
            case n attributes HasId("oid", a) / _          => <span>{ id }</span> % a
            case n attributes HasId("cname", a) / _        => <span>{ cn }</span> % a
            case n attributes HasId("cif", a) / _          => <span>{ cif }</span> % a
            case n attributes HasId("cregcom", a) / _      => <span>{ regCom }</span> % a
            case n attributes HasId("cbank", a) / _        => <span>{ bank }</span> % a
            case n attributes HasId("cbankaccount", a) / _ => <span>{ account }</span> % a
            case n attributes HasId("cregion", a) / _      => <span>{ region }</span> % a
            case n attributes HasId("ccity", a) / _        => <span>{ city }</span> % a
            case n attributes HasId("caddress", a) / _     => <span>{ address }</span> % a
            case n attributes HasId("cemail", a) / _       => <span>{ email }</span> % a
            case n attributes HasId("cphone", a) / _       => <span>{ phone }</span> % a
          } match {
            case Success(n) => Success((s.state.initialState, n))
            case Failure(f) => Success((s.state.initialState, errorTag(f toString)))
          }

      }

  }

  val content = reqSnip("content") {
    s =>
      {
        val items = (NodeSeq.Empty /: s.state.initialState.o.items) {
          case (acc, prod) =>
            ShopApplication.persistence.productById(prod.id) match {
              case Success(p) =>
                (bind(s.node) {
                  case "img" attributes a / _ =>
                    <img/> % a attr ("src", s"http://${Config.string("host")}:${Config.string("port")}${imagePath(prod.id, "normal", p.images.head)}") e
                  case "td" attributes HasClass("c1", a) / _ => <td>{ p.title_?(s.state.lang.name) }</td> % a
                  case "td" attributes HasClass("c2", a) / _ => <td>{ prod.quantity }</td> % a
                  case "td" attributes HasClass("c3", a) / _ => <td>{ p.price }</td> % a
                }) match {
                  case Success(n) => acc ++ n
                  case Failure(f) => acc ++ errorTag(f toString)
                }
              case Failure(f) => errorTag(f getMessage)
            }
        }

        Success(s.state.initialState, items)
      }
  }

  val total = reqSnip("total") {
    s => Success((s.state.initialState, Text(s.state.initialState.o.total.formatted("%.2f"))))
  }
}

case class OrderState(o: OrderLog, lang: Language)

