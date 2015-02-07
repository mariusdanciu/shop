package net.shop
package web.pages

import scala.xml._
import scala.xml._
import net.shift._
import net.shift._
import net.shift.engine.http._
import net.shift.engine.http._
import net.shift.loc._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.web.ShopApplication
import scala.util.Success
import scala.util.Failure
import net.shop.utils.ShopUtils
import net.shift.security.User

object CategoryPage extends Cart[Request] with ShopUtils { self =>

  override def snippets = List(title, item) ++ super.snippets

  val title = reqSnip("title") {
    s => Success((s.state.initialState, <h1>{ Loc.loc0(s.state.lang)("categories").text }</h1>))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ShopApplication.persistence.allCategories match {
          case Success(list) =>
            list flatMap { cat =>
              (bind(s.node) {
                case "li" attributes HasClass("item", a) / childs             => <li>{ childs }</li>
                case "div" attributes HasClass("cat_box", a) / childs         => <div id={ cat stringId } style={ "background: url('" + categoryImagePath(cat) + "') no-repeat" }>{ childs }</div> % a
                case "div" attributes HasClasses("info_tag_text" :: _, a) / _ => <div>{ cat.title_?(s.state.lang.name) }</div> % a
              }) match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
          case Failure(t) => <div class="error">{ Loc.loc0(s.state.lang)("no_categories").text }</div>
        }

        Success(s.state.initialState, prods.toSeq)
      }
  }

}

