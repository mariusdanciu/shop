package net.shop
package web.pages

import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shift.loc.Loc
import net.shift.security.Permission
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template._
import net.shop.model.ProductDetail
import net.shop.utils.ShopUtils._
import net.shop.web.services.ServiceDependencies

import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq, Text}

trait PageCommon[T] extends DynamicContent[T] with ServiceDependencies {

  val connectError = snip[T]("connect_error") {
    s =>
      Success((s.state.initialState, <div id="notice_connect_e">
        {Loc.loc0(s.state.lang)("connect.fail").text}
      </div>))
  }
  val user = snip[T]("user") {
    s =>
      bind(s.node) {
        case Xml(name, attrs, _) =>
          Xml(name, attrs) / (s.state.user.map(u => Text(u.name)).getOrElse(NodeSeq.Empty))
      } map ((s.state.initialState, _))
  }
  val authClass = inline[T]("auth_class") {
    s =>
      val icon = if (s.state.user.isEmpty)
        "icon-login"
      else
        "icon-exit"
      Success((s.state.initialState, icon))
  }
  val logout = inline[T]("logout") {
    s =>
      val icon = if (s.state.user.isEmpty)
        "#"
      else
        "/logout"
      Success((s.state.initialState, icon))
  }
  val permissions = snip[T]("permissions") {
    s =>

      val perms = s.params.map(Permission(_))
      s.state.user match {
        case Some(u) => u.requireAll(perms: _*)((s.state.initialState, s.node)).recover { case t => (s.state.initialState, NodeSeq.Empty) }
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }

  }


  val loggedUser = snip[T]("logged_user") {
    s =>
      s.state.user match {
        case Some(u) => Success(s.state.initialState, s.node)
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }
  }

  val userName = inline[T]("user_name") {
    s =>
      s.state.user match {
        case Some(u) =>
          Success((s.state.initialState, u.name))
        case _ =>
          Success((s.state.initialState, ""))
      }
  }

  snip[T]("user_name") {
    s =>
      s.state.user match {
        case Some(u) =>
          val res = bind(s.node) {
            case Xml("span", a, c) =>
              Xml("span", a, Text(u.name))
          }
          res map {
            (s.state.initialState, _)
          }
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }
  }


  val catMenu = reqSnip("catmenu") {
    s => {
      store.allCategories match {
        case Success(categs) =>
          val cats = categs.toList
          val list = (for {c <- cats} yield {
            bind(s.node) {
              case Xml("a", a, childs) => <a href={s"/products/${nameToPath(c)}"}/> / childs
              case Xml("span", a, childs) => <span>
                {c.title_?(s.state.lang.name)}
              </span>
            } getOrElse Nil
          }).flatten

          Success(s.state.initialState, NodeSeq.fromSeq(list))
        case Failure(t) =>
          Success(s.state.initialState, errorTag(Loc.loc0(s.state.lang)(t.getMessage).text))
      }
    }
  }

  override def inlines = List(authClass, logout, userName)

  def snippets = List(connectError, user, permissions, loggedUser, catMenu)

  def reqSnip(name: String) = snip[T](name) _

  def priceTag(p: ProductDetail): Elem = {
    p.discountPrice match {
      case Some(discount) => <span>
        {<span>
          {price(discount)}
        </span> ++ <strike>
          {price(p.price)}
        </strike> <span>Lei</span>}
      </span>
      case _ => <span>
        {s"${price(p.price)} Lei"}
      </span>
    }
  }
}