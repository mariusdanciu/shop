package net.shop.web.pages

import net.shift.common.XmlImplicits._
import net.shift.common.{Path, Xml}
import net.shift.engine.page.Html5
import net.shift.loc.{Language, Loc}
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Template._
import net.shift.template._
import net.shop.model._
import net.shop.utils.NormalPic
import net.shop.utils.ShopUtils._
import net.shop.web.services.ServiceDependencies

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

trait OrderPage extends DynamicContent[OrderState] with ServiceDependencies {

  val logoUrl = inline[OrderState]("logo_url") {
    s => Success((s.state.initialState, s"http://${cfg.string("host")}:${cfg.string("port")}/static/images/logo.png"))
  }
  val info = reqSnip("info") {
    s =>
      s.state.initialState.o match {
        case OrderLog(id, time, Person(fn, ln, cnp), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
          bind(s.node) {
            case Xml(n, HasId("oid", a), _)     => <span>{ id }</span> % a
            case Xml(n, HasId("lname", a), _)   => <span>{ ln }</span> % a
            case Xml(n, HasId("fname", a), _)   => <span>{ fn }</span> % a
            case Xml(n, HasId("cnp", a), _)     => <span>{ cnp }</span> % a
            case Xml(n, HasId("region", a), _)  => <span>{ region }</span> % a
            case Xml(n, HasId("city", a), _)    => <span>{ city }</span> % a
            case Xml(n, HasId("address", a), _) => <span>{ address }</span> % a
            case Xml(n, HasId("email", a), _)   => <span>{ email }</span> % a
            case Xml(n, HasId("phone", a), _)   => <span>{ phone }</span> % a
          } match {
            case Success(n) => Success((s.state.initialState, n))
            case Failure(f) => Success((s.state.initialState, errorTag(f toString)))
          }

        case OrderLog(id, time, Company(cn, cif, regCom, bank, account), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
          bind(s.node) {
            case Xml(n, HasId("oid", a), _)          => <span>{ id }</span> % a
            case Xml(n, HasId("cname", a), _)        => <span>{ cn }</span> % a
            case Xml(n, HasId("cif", a), _)          => <span>{ cif }</span> % a
            case Xml(n, HasId("cregcom", a), _)      => <span>{ regCom }</span> % a
            case Xml(n, HasId("cbank", a), _)        => <span>{ bank }</span> % a
            case Xml(n, HasId("cbankaccount", a), _) => <span>{ account }</span> % a
            case Xml(n, HasId("cregion", a), _)      => <span>{ region }</span> % a
            case Xml(n, HasId("ccity", a), _)        => <span>{ city }</span> % a
            case Xml(n, HasId("caddress", a), _)     => <span>{ address }</span> % a
            case Xml(n, HasId("cemail", a), _)       => <span>{ email }</span> % a
            case Xml(n, HasId("cphone", a), _)       => <span>{ phone }</span> % a
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
            store.productById(prod.id) match {
              case Success(p) =>
                bind(s.node) {
                  case Xml("a", HasClass("page", a), childs) =>
                    (<a href={s"http://${cfg.string("host")}:${cfg.string("port")}${productPage(prod.id)}"}/> % a) / childs
                  case Xml("img", a, _) =>
                    <img/> % (a + ("src", s"http://${cfg.string("host")}:${cfg.string("port")}${imagePath(NormalPic, prod.id)}"))
                  case Xml("td", HasClass("c1", a), _) => <td>{ p.title_?(s.state.lang.name) }</td> % a
                  case Xml("td", HasClass("c2", a), _) => <td>{ prod.quantity }</td> % a
                  case Xml("td", HasClass("c3", a), _) => <td>{ p.actualPrice }</td> % a
                } match {
                  case Success(n)                 => acc ++ n
                  case Failure(ShopError(msg, _)) => acc ++ errorTag(Loc.loc0(s.state.lang)(msg).text)
                  case Failure(f)                 => acc ++ errorTag(f toString)
                }
              case Failure(f) => errorTag(f getMessage)
            }
        }

        Success(s.state.initialState, items)
      }
  }
  val total = inline[OrderState]("total") {
    s =>
      Success((s.state.initialState, price(s.state.initialState.o.total + s.state.initialState.o.transport.price)))
  }
  val transport = inline[OrderState]("transport") {
    s =>
      Success((s.state.initialState, s.state.initialState.o.transport.name))
  }

  override def inlines = List(logoUrl, transport, total)

  override def snippets = List(info, content)

  def reqSnip(name: String) = snip[OrderState](name) _

  def orderTemplate(state: OrderState): Try[NodeSeq] = {
    Html5.runPageFromFile(PageState(state, state.lang), Path(s"web/templates/order_${state.lang.name}.html"), this).map(in => in._2)
  }
}

case class OrderState(o: OrderLog, lang: Language)

