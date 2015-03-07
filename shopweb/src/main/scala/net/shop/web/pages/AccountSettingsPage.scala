package net.shop.web.pages

import net.shift.loc.Loc
import net.shop.utils.ShopUtils
import net.shift.engine.http.Request
import scala.util.Success
import net.shift.template.Binds._
import net.shift.common.XmlUtils._
import net.shift.template.HasName
import net.shift.common.NodeOps._
import net.shift.common.NodeOps
import net.shop.web.ShopApplication
import scala.xml.NodeSeq
import net.shift.template.PageState
import net.shift.engine.page.Html5
import net.shift.common.Path
import net.shop.api.UserDetail
import net.shift.template.DynamicContent
import net.shop.api.Address
import net.shift.template.Selectors
import net.shift.template.Snippet._
import net.shift.template.HasClass
import scala.xml.Text
import scala.xml.Elem
import net.shift.template.Attributes
import net.shift.io.IODefaults
import net.shift.common.XmlUtils

object AccountSettingsPage extends Cart[SettingsPageState] with ShopUtils with IODefaults { self =>

  override def snippets = List(title, settingsForm, addressTemplate, addresses) ++ super.snippets

  val title = reqSnip("title") {
    s => Success((s.state.initialState, <h1>{ Loc.loc0(s.state.lang)("settings").text }</h1>))
  }

  val settingsForm = reqSnip("settingsForm") {
    s =>
      {
        s.state.user match {
          case Some(user) =>
            val r = ShopApplication.persistence.userByEmail(user.name) match {
              case scala.util.Success(ud) =>
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
}

case class SettingsPageState(req: Request, user: Option[UserDetail])

object AddressPage extends DynamicContent[Address] with ShopUtils with Selectors { self =>
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