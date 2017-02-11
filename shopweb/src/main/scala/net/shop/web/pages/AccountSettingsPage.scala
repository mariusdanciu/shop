package net.shop
package web.pages

import net.shift.common.{Path, Xml, XmlAttr}
import net.shift.common.XmlImplicits._
import net.shift.loc.Loc
import net.shift.security.Permission
import net.shift.server.http.Request
import net.shift.template.Binds.bind
import net.shift.template.{HasClass, HasId, HasName, HasValue}
import net.shop.api._
import net.shop.utils.ShopUtils._
import net.shop.utils.ThumbPic

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{NodeSeq, Text}

trait AccountSettingsPage extends PageCommon[SettingsPageState] {
  self =>

  val dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy - hh:mm")
  val userInfo = reqSnip("userInfo") {
    s =>
      {
        s.state.user match {
          case Some(user) =>
            val r = store.userByEmail(user.name) match {
              case scala.util.Success(Some(ud)) =>
                (Some(ud), bind(s.node) {
                  case HasName("update_firstName", a) => Xml("input", a + ("value", ud.userInfo.firstName))
                  case HasName("update_lastName", a)  => Xml("input", a + ("value", ud.userInfo.lastName))
                  case HasName("update_cnp", a)       => Xml("input", a + ("value", ud.userInfo.cnp))
                  case HasName("update_phone", a)     => Xml("input", a + ("value", ud.userInfo.phone))
                })
              case _ => (None, Success(NodeSeq.Empty))
            }

            r._2.map(p => ((SettingsPageState(s.state.initialState.req, r._1), p)))
          case _ =>
            Success((s.state.initialState, s.node))
        }
      }
  }
  val address = reqSnip("address") {
    s =>
      {
        val res: NodeSeq = s.state.initialState.user match {
          case Some(u) =>

            val addr = u.addresses match {
              case Nil    => Address(None, "", "", "", "", "", "")
              case h :: _ => h
            }

            val ak = bind(s.node) {
              case HasName("addr_region", a) => Xml("input", a + ("value", addr.region))
              case HasName("addr_city", a)   => Xml("input", a + ("value", addr.city))
              case HasName("addr_addr", a)   => Xml("input", a + ("value", addr.address))
              case HasName("addr_zip", a)    => Xml("input", a + ("value", addr.zipCode))
            }
            ak match {
              case Success(n) => n
              case _          => NodeSeq.Empty
            }
          case _ => NodeSeq.Empty
        }
        Success((s.state.initialState, res))
      }
  }
  val orders = reqSnip("orders") {
    s =>
      {
        val email = s.state.initialState.req.uri.param("email").map { _.value.head }.getOrElse("")

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
          Path(s.state.initialState.req.uri.path) match {
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

                bind(s.node.head.child) {

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

                  case HasClass("order_title", a) => Xml("a", a) / Text(s"Comanda ${o.id} din data ${dateFormat.format(o.time)}")
                  case Xml(_, HasClass("order_items", a), childs) =>

                    o.items flatMap { item =>
                      <tr> {
                        val prod = store.productById(item.id)
                        val title = prod.map { _.title_?(s.state.lang.name) } getOrElse ""
                        def img(a: XmlAttr): NodeSeq = prod.map { p => Xml("img", a + ("src", imagePath(ThumbPic, item.id))) } getOrElse NodeSeq.Empty

                        bind(childs) {
                          case HasClass("c1", a)               => img(a)
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

                } getOrElse NodeSeq.Empty
              }
              (SettingsPageState(s.state.initialState.req, Some(u)), NodeSeq.fromSeq(v.toSeq))
            }
          case Success(None) => Success((s.state.initialState, Text(Loc.loc(s.state.lang)("user.not.found", List(email)).text)))
          case Failure(t)    => Failure(t)
        }

      }
  }
  val users = reqSnip("users") {
    s =>
      {
        store.allUsers match {
          case Success(users) =>
            val n = users.flatMap { user =>
              bind(s.node.head.child) {
                case HasClass("user_title", a)    => <span>{ user.email }</span>

                case HasId("update_firstName", a) => Xml("span", a) / Text(user.userInfo.firstName)
                case HasId("update_lastName", a)  => Xml("span", a) / Text(user.userInfo.lastName)
                case HasId("update_cnp", a)       => Xml("span", a) / Text(user.userInfo.cnp)
                case HasId("update_phone", a)     => Xml("span", a) / Text(user.userInfo.phone)

                case HasId("addresses", a) =>
                  user.addresses.flatMap { addr =>
                    <li>{ s"${addr.address}, ${addr.city}, ${addr.region}, ${addr.country}, ${addr.zipCode}" }</li>
                  }

                case Xml(n, HasClass("deleteuser", _), childs) =>
                  bind(childs) {
                    case Xml(n, a, _) => Xml(n, a + ("data-email", user.email))
                  } getOrElse NodeSeq.Empty

              } getOrElse NodeSeq.Empty
            }
            Success((s.state.initialState, NodeSeq.fromSeq(n.toSeq)))

          case Failure(t) => Failure(t)
        }
      }
  }

  override def snippets = List(userInfo, address, orders, users) ++ super.snippets

  private def optsToNode(opts: Map[String, String]): NodeSeq = {
    NodeSeq.fromSeq((for {(k, v) <- opts} yield {
      <div class="row_simple">
        <span>
          {k + " : " + v}
        </span>
      </div>
    }).toSeq)
  }

  private def makeSelect(o: OrderLog, childs: NodeSeq): NodeSeq = {

    bind(childs) {
      case Xml("option", HasValue("0", a), _) if (o.status == OrderReceived)  => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("1", a), _) if (o.status == OrderPending)   => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("2", a), _) if (o.status == OrderFinalized) => Xml("option", a + ("selected", "true"))
      case Xml("option", HasValue("-1", a), _) if (o.status == OrderCanceled) => Xml("option", a + ("selected", "true"))
    } match {
      case Success(n) => <select id={ o.id } class="order_edit_status">
                           { n }
                         </select>
      case _ => NodeSeq.Empty
    }

  }

}

case class SettingsPageState(req: Request, user: Option[UserDetail])
