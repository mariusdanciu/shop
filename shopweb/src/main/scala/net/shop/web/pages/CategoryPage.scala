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

  def snippets = List(cartPopup, item)

  def reqSnip(name: String) = snip[Request](name) _

  val cartPopup = reqSnip("cart_popup") {
    s => cartTemplate(s.state, s.state) map { c => (s.state, c) }
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ShopApplication.productsService.allCategories() match {
          case Success(list) =>
            println(list)
            val v = list flatMap { cat =>
              (bind(s.node) {
                case "li" > (a / childs) if (a hasClass "item") => <li>{ childs }</li>
                case "a" > (attrs / childs) => <a id={ cat id } href={ "/products?cat=" + cat.id }>{ childs }</a>
                case "div" > (a / childs) if (a hasClass "cat_box") => <div title={ cat title } style={ "background-image: url('" + categoryImagePath(cat) + "')" }>{ childs }</div> % a
                case "div" > (a / _) if (a hasClass "info_tag_text") => <div>{ cat title }</div> % a
              }) match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
            println(v)
            v
          case Failure(t) => <div class="error">{ Loc.loc0(s.state.language)("no_categories").text }</div>
        }

        Success(s.state, prods.toSeq)
      }
  }

}
