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
import net.shift.template.Snippet._
import utils.ShopUtils._
import net.shop.web.ShopApplication
import scala.util.Success
import scala.util.Failure

object CategoryPage extends Cart[Request] { self =>

  override def snippets = List(title, item) ++ super.snippets

  val title = reqSnip("title") {
    s => Success((s.state, <h1>{Loc.loc0(s.language)("categories").text}</h1>))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ShopApplication.productsService.allCategories match {
          case Success(list) =>
            list flatMap { cat =>
              (bind(s.node) {
                case "li" :/ a / childs if (a hasClass "item") => <li>{ childs }</li>
                case "a" :/ attrs / childs => <a id={ cat id } href={ "/products?cat=" + cat.id }>{ childs }</a>
                case "div" :/ a / childs if (a hasClass "cat_box") => <div title={ cat.title_?(s.language) } style={ "background-image: url('" + categoryImagePath(cat) + "')" }>{ childs }</div> % a
                case "div" :/ HasClasses("info_tag_text" :: _, a) / _ => <div>{ cat.title_?(s.language) }</div> % a
              }) match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
          case Failure(t) => <div class="error">{ Loc.loc0(s.language)("no_categories").text }</div>
        }

        Success(s.state, prods.toSeq)
      }
  }

}
