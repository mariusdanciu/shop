package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Elem
import net.shift.common.NodeOps.node
import net.shift.common.Path
import net.shift.common.XmlUtils.elem2NodeOps
import net.shift.common.XmlUtils.nodeOps2Elem
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.io.IODefaults
import net.shift.loc.Loc
import net.shift.security.Permission
import net.shift.template.Attributes
import net.shift.template.Binds.attrs2MetaData
import net.shift.template.Binds.bind
import net.shift.template.DynamicContent
import net.shift.template.HasClass
import net.shift.template.HasId
import net.shift.template.HasName
import net.shift.template.HasValue
import net.shift.template.PageState
import net.shift.template.Selectors
import net.shift.template.Snippet.snip
import net.shop.api.Address
import net.shop.api.OrderCanceled
import net.shop.api.OrderFinalized
import net.shop.api.OrderLog
import net.shop.api.OrderPending
import net.shop.api.OrderReceived
import net.shop.api.UserDetail
import net.shop.utils.ShopUtils._
import net.shop.web.ShopApplication
import net.shift.template.Binds._
import net.shift.common.XmlUtils._
import net.shop.api.Person
import net.shop.api.Company

object AccountSettingsPage extends Cart[SettingsPageState] with IODefaults { self =>

  val dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy - hh:mm")

  override def snippets = List(settingsForm, addressTemplate, addresses, orders) ++ super.snippets

  def pageTitle(s: PageState[SettingsPageState]) = Loc.loc0(s.lang)("settings").text

  val settingsForm = reqSnip("settingsForm") {
    s =>
      {
        s.state.user match {
          case Some(user) =>
            val r = ShopApplication.persistence.userByEmail(user.name) match {
              case scala.util.Success(Some(ud)) =>
                (Some(ud), bind(s.node) {
                  case HasName("update_firstName", a)    => node("input", a.attrs + ("value" -> ud.userInfo.firstName))
                  case HasName("update_lastName", a)     => node("input", a.attrs + ("value" -> ud.userInfo.lastName))
                  case HasName("update_cnp", a)          => node("input", a.attrs + ("value" -> ud.userInfo.cnp))
                  case HasName("update_phone", a)        => node("input", a.attrs + ("value" -> ud.userInfo.phone))

                  case HasName("update_cname", a)        => node("input", a.attrs + ("value" -> ud.companyInfo.name))
                  case HasName("update_cif", a)          => node("input", a.attrs + ("value" -> ud.companyInfo.cif))
                  case HasName("update_cregcom", a)      => node("input", a.attrs + ("value" -> ud.companyInfo.regCom))
                  case HasName("update_cbank", a)        => node("input", a.attrs + ("value" -> ud.companyInfo.bank))
                  case HasName("update_cbankaccount", a) => node("input", a.attrs + ("value" -> ud.companyInfo.bankAccount))
                  case HasName("update_cphone", a)       => node("input", a.attrs + ("value" -> ud.companyInfo.phone))
                })
              case _ => (None, Success(NodeSeq.Empty))
            }

            r._2.map(p => ((SettingsPageState(s.state.initialState.req, r._1), p)))
          case _ =>
            Success((s.state.initialState, s.node))
        }
      }
  }

  val addressTemplate = reqSnip("address_template") {
    s =>
      {
        val res = Html5.runPageFromFile(PageState(Address(None, "", "", "", "", "", ""),
          s.state.initialState.req.language),
          Path("web/templates/address.html"), AddressPage).map(_._2)

        res.map(((s.state.initialState, _)))
      }
  }

  val addresses = reqSnip("addresses") {
    s =>
      {
        val res: NodeSeq = s.state.initialState.user match {
          case Some(u) => u.addresses.flatMap { addr =>
            val ak = Html5.runPageFromFile(PageState(addr,
              s.state.initialState.req.language),
              Path("web/templates/address.html"), AddressPage).map(_._2)
            ak match {
              case Success(n) => n.head match {
                case e: Elem => e.child
                case _       => NodeSeq.Empty
              }
              case _ => NodeSeq.Empty
            }
          }
          case _ => NodeSeq.Empty
        }
        Success((s.state.initialState, res))
      }
  }

  val orders = reqSnip("orders") {
    s =>
      {
        val email = s.state.initialState.req.param("email").getOrElse(List("")).head
        def userDetail(state: SettingsPageState): Try[Option[UserDetail]] = {
          state.user match {
            case Some(u) => Success(Some(u))
            case _ =>
              if (!email.isEmpty())
                ShopApplication.persistence.userByEmail(email)
              else
                ShopApplication.persistence.userByEmail(s.state.user.map { _ name } getOrElse "")
          }
        }

        def query(u: UserDetail) =
          s.state.initialState.req.path match {
            case Path(_, _ :: "received" :: Nil) => ShopApplication.persistence.ordersByStatus(OrderReceived)
            case Path(_, _ :: "pending" :: Nil)  => ShopApplication.persistence.ordersByStatus(OrderPending)
            case _                               => ShopApplication.persistence.ordersByEmail(u.email)
          }

        val loggedinUser = s.state.user
        val l = s.state.lang

        userDetail(s.state.initialState) match {
          case Success(Some(u)) =>
            for {
              orders <- query(u)
            } yield {

              val v = orders.flatMap { o =>

                (bind(s.node.head.child) {

                  case _ attributes HasId("person_info", a) / childs => o match {
                    case OrderLog(id, time, Person(fn, ln, cnp), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
                      bind(childs) {
                        case n attributes HasId("oid", a) / _     => <span>{ id }</span> % a
                        case n attributes HasId("lname", a) / _   => <span>{ ln }</span> % a
                        case n attributes HasId("fname", a) / _   => <span>{ fn }</span> % a
                        case n attributes HasId("cnp", a) / _     => <span>{ cnp }</span> % a
                        case n attributes HasId("region", a) / _  => <span>{ region }</span> % a
                        case n attributes HasId("city", a) / _    => <span>{ city }</span> % a
                        case n attributes HasId("address", a) / _ => <span>{ address }</span> % a
                        case n attributes HasId("email", a) / _   => <span>{ email }</span> % a
                        case n attributes HasId("phone", a) / _   => <span>{ phone }</span> % a
                      } getOrElse NodeSeq.Empty
                    case _ => NodeSeq.Empty
                  }

                  case _ attributes HasId("company_info", a) / childs => o match {
                    case OrderLog(id, time, Company(cn, cif, regCom, bank, account), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
                      bind(childs) {
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
                      } getOrElse NodeSeq.Empty
                    case _ => NodeSeq.Empty
                  }

                  case HasClass("order_title", a) => node("a", a.attrs) / Text(s"Comanda ${o.id} din data ${dateFormat.format(o.time)}")
                  case _ attributes HasClass("order_items", a) / childs =>

                    o.items flatMap { item =>
                      <tr> {
                        val prod = ShopApplication.persistence.productById(item.id)
                        val title = prod.map { _.title_?(s.state.lang.name) } getOrElse ""
                        def img(a: Attributes): NodeSeq = prod.map { p => node("img", a.attrs + ("src" -> imagePath(item.id, "thumb", p.images.head))) } getOrElse NodeSeq.Empty

                        bind(childs) {
                          case HasClass("c1", a)                     => img(a)
                          case "td" attributes HasClass("c2", a) / _ => <td>{ title }</td> % a
                          case "td" attributes HasClass("c3", a) / _ => <td>{ item.quantity }</td> % a
                          case "td" attributes HasClass("c4", a) / _ => <td>{ price(item.price) }</td> % a
                        } getOrElse NodeSeq.Empty
                      }</tr>
                    }

                  case HasClass("transport", a) =>
                    Text(o.transport.name)

                  case HasClass("total", a) => Text(price(((0.0 /: o.items)((acc, e) => acc + e.price * e.quantity)) + o.transport.price))

                  case HasClass("status", a) =>
                    val e = for {
                      u <- option2Try(loggedinUser)
                      n <- u.notThesePermissions(Permission("write")) {
                        NodeSeq.Empty ++ (node("span", Map("class" -> "order_status")) / (o.status match {
                          case OrderReceived  => Text(Loc.loc0(l)("received").text)
                          case OrderPending   => Text(Loc.loc0(l)("pending").text)
                          case OrderFinalized => Text(Loc.loc0(l)("finalized").text)
                          case OrderCanceled  => Text(Loc.loc0(l)("canceled").text)
                        }))
                      }
                    } yield n

                    e getOrElse NodeSeq.Empty

                  case _ attributes HasClass("edit_status", a) / childs =>
                    val e = for {
                      u <- option2Try(loggedinUser)
                      n <- u.requireAll(Permission("write")) {
                        makeSelect(o, childs)
                      }
                    } yield n

                    e getOrElse NodeSeq.Empty

                }) getOrElse NodeSeq.Empty
              }
              (SettingsPageState(s.state.initialState.req, Some(u)), NodeSeq.fromSeq(v.toSeq))
            }
          case Success(None) => Success((s.state.initialState, Text(Loc.loc(s.state.lang)("user.not.found", List(email)).text)))
          case Failure(t)    => Failure(t)
        }

      }
  }

  private def makeSelect(o: OrderLog, childs: NodeSeq): NodeSeq = {

    bind(childs) {
      case "option" attributes HasValue("0", a) / _ if (o.status == OrderReceived)  => node("option", a.attrs + ("selected" -> "true"))
      case "option" attributes HasValue("1", a) / _ if (o.status == OrderPending)   => node("option", a.attrs + ("selected" -> "true"))
      case "option" attributes HasValue("2", a) / _ if (o.status == OrderFinalized) => node("option", a.attrs + ("selected" -> "true"))
      case "option" attributes HasValue("-1", a) / _ if (o.status == OrderCanceled) => node("option", a.attrs + ("selected" -> "true"))
    } match {
      case Success(n) => <select id={ o.id } class="order_edit_status">
                           { n }
                         </select>
      case _ => NodeSeq.Empty
    }

  }

}

case class SettingsPageState(req: Request, user: Option[UserDetail])

object AddressPage extends DynamicContent[Address] with Selectors { self =>
  override def snippets = List(addr)

  val addr = snip[Address]("addr") {
    s =>
      {
        def augmentAttr(in: Map[String, String], name: String, s: String): String = in.get(name).map(_ + ":" + s).getOrElse("")

        val name = s.state.initialState.name

        def augmentInput(a: Attributes, v: String) = node("input", a.attrs +
          ("value" -> v) +
          ("id" -> augmentAttr(a.attrs, "id", name)) +
          ("name" -> augmentAttr(a.attrs, "name", name)))

        val res = bind(s.node) {
          case HasClass("addr_title", a) => node("a", a.attrs) / Text(name)
          case "label" attributes a      => node("label", a.attrs.attrs + ("for" -> augmentAttr(a.attrs.attrs, "for", name)))
          case HasName("addr_region", a) => augmentInput(a, s.state.initialState.region)
          case HasName("addr_city", a)   => augmentInput(a, s.state.initialState.city)
          case HasName("addr_addr", a)   => augmentInput(a, s.state.initialState.address)
          case HasName("addr_zip", a)    => augmentInput(a, s.state.initialState.zipCode)
        }
        res.map((s.state.initialState, _))
      }
  }
}