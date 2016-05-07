package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Elem
import net.shift.common.Path
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.io.IODefaults
import net.shift.loc.Loc
import net.shift.security.Permission
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
import net.shift.common.Xml
import net.shift.common.XmlAttr
import net.shift.common.XmlImplicits._
import net.shop.web.services.ServiceDependencies

trait AccountSettingsPage extends Cart[SettingsPageState] with IODefaults with ServiceDependencies { self =>

  val dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy - hh:mm")

  override def snippets = List(settingsForm, addressTemplate, addresses, orders, users) ++ super.snippets

  val settingsForm = reqSnip("settingsForm") {
    s =>
      {
        s.state.user match {
          case Some(user) =>
            val r = store.userByEmail(user.name) match {
              case scala.util.Success(Some(ud)) =>
                (Some(ud), bind(s.node) {
                  case HasName("update_firstName", a)    => Xml("input", a + ("value", ud.userInfo.firstName))
                  case HasName("update_lastName", a)     => Xml("input", a + ("value", ud.userInfo.lastName))
                  case HasName("update_cnp", a)          => Xml("input", a + ("value", ud.userInfo.cnp))
                  case HasName("update_phone", a)        => Xml("input", a + ("value", ud.userInfo.phone))

                  case HasName("update_cname", a)        => Xml("input", a + ("value", ud.companyInfo.name))
                  case HasName("update_cif", a)          => Xml("input", a + ("value", ud.companyInfo.cif))
                  case HasName("update_cregcom", a)      => Xml("input", a + ("value", ud.companyInfo.regCom))
                  case HasName("update_cbank", a)        => Xml("input", a + ("value", ud.companyInfo.bank))
                  case HasName("update_cbankaccount", a) => Xml("input", a + ("value", ud.companyInfo.bankAccount))
                  case HasName("update_cphone", a)       => Xml("input", a + ("value", ud.companyInfo.phone))

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
                store.userByEmail(email)
              else
                store.userByEmail(s.state.user.map { _ name } getOrElse "")
          }
        }

        def query(u: UserDetail) =
          s.state.initialState.req.path match {
            case Path(_, _ :: "received" :: Nil) => store.ordersByStatus(OrderReceived)
            case Path(_, _ :: "pending" :: Nil)  => store.ordersByStatus(OrderPending)
            case _                               => store.ordersByEmail(u.email)
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

                  case Xml(_, HasId("person_info", a), childs) => o match {
                    case OrderLog(id, time, Person(fn, ln, cnp), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
                      bind(childs) {
                        case Xml(_, HasId("oid", a), _)     => <span>{ id }</span> % a
                        case Xml(_, HasId("lname", a), _)   => <span>{ ln }</span> % a
                        case Xml(_, HasId("fname", a), _)   => <span>{ fn }</span> % a
                        case Xml(_, HasId("cnp", a), _)     => <span>{ cnp }</span> % a
                        case Xml(_, HasId("region", a), _)  => <span>{ region }</span> % a
                        case Xml(_, HasId("city", a), _)    => <span>{ city }</span> % a
                        case Xml(_, HasId("address", a), _) => <span>{ address }</span> % a
                        case Xml(_, HasId("email", a), _)   => <span>{ email }</span> % a
                        case Xml(_, HasId("phone", a), _)   => <span>{ phone }</span> % a
                      } getOrElse NodeSeq.Empty
                    case _ => NodeSeq.Empty
                  }

                  case Xml(_, HasId("company_info", a), childs) => o match {
                    case OrderLog(id, time, Company(cn, cif, regCom, bank, account), Address(_, _, country, region, city, address, zip), email, phone, _, _, _) =>
                      bind(childs) {
                        case Xml(_, HasId("oid", a), _)          => <span>{ id }</span> % a
                        case Xml(_, HasId("cname", a), _)        => <span>{ cn }</span> % a
                        case Xml(_, HasId("cif", a), _)          => <span>{ cif }</span> % a
                        case Xml(_, HasId("cregcom", a), _)      => <span>{ regCom }</span> % a
                        case Xml(_, HasId("cbank", a), _)        => <span>{ bank }</span> % a
                        case Xml(_, HasId("cbankaccount", a), _) => <span>{ account }</span> % a
                        case Xml(_, HasId("cregion", a), _)      => <span>{ region }</span> % a
                        case Xml(_, HasId("ccity", a), _)        => <span>{ city }</span> % a
                        case Xml(_, HasId("caddress", a), _)     => <span>{ address }</span> % a
                        case Xml(_, HasId("cemail", a), _)       => <span>{ email }</span> % a
                        case Xml(_, HasId("cphone", a), _)       => <span>{ phone }</span> % a
                      } getOrElse NodeSeq.Empty
                    case _ => NodeSeq.Empty
                  }

                  case HasClass("order_title", a) => Xml("a", a) / Text(s"Comanda ${o.id} din data ${dateFormat.format(o.time)}")
                  case Xml(_, HasClass("order_items", a), childs) =>

                    o.items flatMap { item =>
                      <tr> {
                        val prod = store.productById(item.id)
                        val title = prod.map { _.title_?(s.state.lang.name) } getOrElse ""
                        def img(a: XmlAttr): NodeSeq = prod.map { p => Xml("img", a + ("src", imagePath(item.id, "thumb", p.images.head))) } getOrElse NodeSeq.Empty

                        bind(childs) {
                          case HasClass("c1", a)                     => img(a)
                          case Xml("td", HasClass("c2", a), _) => <td>{ title }</td> % a
                          case Xml("td", HasClass("c3", a), _) => <td>{ item.quantity }</td> % a
                          case Xml("td", HasClass("c4", a), _) => <td>{ price(item.price) }</td> % a
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
                        NodeSeq.Empty ++ (Xml("span", XmlAttr("class", "order_status")) / (o.status match {
                          case OrderReceived  => Text(Loc.loc0(l)("received").text)
                          case OrderPending   => Text(Loc.loc0(l)("pending").text)
                          case OrderFinalized => Text(Loc.loc0(l)("finalized").text)
                          case OrderCanceled  => Text(Loc.loc0(l)("canceled").text)
                        }))
                      }
                    } yield n

                    e getOrElse NodeSeq.Empty

                  case Xml(_, HasClass("edit_status", a), childs) =>
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

  private def optsToNode(opts: Map[String, String]): NodeSeq = {
    NodeSeq.fromSeq((for { (k, v) <- opts } yield {
      <div class="row_simple">
        <span>{ k + " : " + v }</span>
      </div>
    }).toSeq)
  }

  val users = reqSnip("users") {
    s =>
      {
        store.allUsers match {
          case Success(users) =>
            val n = users.flatMap { user =>
              bind(s.node.head.child) {
                case HasClass("user_title", a)       => <span>{ user.email }</span>

                case HasId("update_firstName", a)    => Xml("span", a) / Text(user.userInfo.firstName)
                case HasId("update_lastName", a)     => Xml("span", a) / Text(user.userInfo.lastName)
                case HasId("update_cnp", a)          => Xml("span", a) / Text(user.userInfo.cnp)
                case HasId("update_phone", a)        => Xml("span", a) / Text(user.userInfo.phone)

                case HasId("update_cname", a)        => Xml("span", a) / Text(user.companyInfo.name)
                case HasId("update_cif", a)          => Xml("span", a) / Text(user.companyInfo.cif)
                case HasId("update_cregcom", a)      => Xml("span", a) / Text(user.companyInfo.regCom)
                case HasId("update_cbank", a)        => Xml("span", a) / Text(user.companyInfo.bank)
                case HasId("update_cbankaccount", a) => Xml("span", a) / Text(user.companyInfo.bankAccount)
                case HasId("update_cphone", a)       => Xml("span", a) / Text(user.companyInfo.phone)

                case HasId("addresses", a) =>
                  user.addresses.flatMap { addr =>
                    <li>{ s"${addr.address}, ${addr.city}, ${addr.region}, ${addr.country}, ${addr.zipCode}" }</li>
                  }

                case Xml(n, HasClass("deleteuser", _), childs) =>
                  bind(childs) {
                    case Xml(n, a,  _) => Xml(n, a + ("data-email", user.email))
                  } getOrElse NodeSeq.Empty

              } getOrElse NodeSeq.Empty
            }
            Success((s.state.initialState, NodeSeq.fromSeq(n.toSeq)))

          case Failure(t) => Failure(t)
        }
      }
  }

  private def makeSelect(o: OrderLog, childs: NodeSeq): NodeSeq = {

    bind(childs) {
      case Xml("option", HasValue("0", a), _ ) if (o.status == OrderReceived)  => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("1", a), _ ) if (o.status == OrderPending)   => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("2", a), _ ) if (o.status == OrderFinalized) => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("-1", a), _ ) if (o.status == OrderCanceled) => Xml("option", a + ("selected", "true"))
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

        def augmentInput(a: XmlAttr, v: String) = Xml("input", a +
          ("value", v) +
          ("id", augmentAttr(a.attrs, "id", name)) +
          ("name", augmentAttr(a.attrs, "name", name)))

        val res = bind(s.node) {
          case HasClass("addr_title", a) => Xml("a", a) / Text(name)
          case Xml("label", a, _)      => Xml("label", a + ("for", augmentAttr(a.attrs, "for", name)))
          case HasName("addr_region", a) => augmentInput(a, s.state.initialState.region)
          case HasName("addr_city", a)   => augmentInput(a, s.state.initialState.city)
          case HasName("addr_addr", a)   => augmentInput(a, s.state.initialState.address)
          case HasName("addr_zip", a)    => augmentInput(a, s.state.initialState.zipCode)
        }
        res.map(r => (s.state.initialState, r))
      }
  }
}