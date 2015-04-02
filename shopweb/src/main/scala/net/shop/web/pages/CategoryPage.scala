package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
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
import net.shop.utils.ShopUtils
import net.shop.web.ShopApplication

object CategoryPage extends Cart[Request] with ShopUtils { self =>

  override def snippets = List(title, item) ++ super.snippets

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("categories").text

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

