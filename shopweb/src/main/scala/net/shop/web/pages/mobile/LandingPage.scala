package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies
import net.shop.web.pages.Cart
import net.shift.engine.http.Request
import net.shift.loc.Loc
import scala.util.Failure
import scala.util.Success
import net.shift.template.HasClasses
import net.shift.template.HasClass
import net.shop.api.ShopError
import net.shift.common.Xml
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.utils.ShopUtils._

trait LandingPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List(cat) ++ super.snippets

  val cat = reqSnip("categories") {
    s =>
      {
        val prods = store.allCategories match {
          case Success(list) =>
            list flatMap { cat =>
              (bind(s.node) {
                case Xml("span", HasId("title", attrs), childs) => <span>{ cat.title_?(s.state.lang.name) }</span>
              }) match {
                case Success(n)                 => n
                case Failure(ShopError(msg, _)) => errorTag(Loc.loc0(s.state.lang)(msg).text)
                case Failure(f)                 => errorTag(f toString)
              }
            }
          case Failure(t) => <div class="error">{ Loc.loc0(s.state.lang)("no.categories").text }</div>
        }

        Success(s.state.initialState, prods.toSeq)
      }
  }
}