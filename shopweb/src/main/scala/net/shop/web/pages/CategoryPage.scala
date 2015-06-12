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
import net.shop.utils.ShopUtils._
import net.shop.web.ShopApplication
import net.shop.api.ShopError
import net.shift.common.BNode
import net.shift.common.BNodeImplicits._


object CategoryPage extends Cart[Request] { self =>

  override def snippets = List(title, item) ++ super.snippets

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("categories").text

  val item = reqSnip("item") {
    s =>
      {
        val prods = ShopApplication.persistence.allCategories match {
          case Success(list) =>
            list flatMap { cat =>
              (bind(s.node) {
                case BNode("li", HasClass("item", a), childs)             => <li>{ childs }</li>
                case BNode("div", HasClass("cat_box", a), childs)         => <div id={ cat stringId } style={ "background: url('" + categoryImagePath(cat) + "') no-repeat" }>{ childs }</div> % a
                case BNode("div", HasClasses("info_tag_text" :: _, a), _) => <div>{ cat.title_?(s.state.lang.name) }</div> % a
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

