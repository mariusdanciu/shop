package net.shop
package web.pages

import scala.xml._
import net.shift._
import template._
import engine.http._
import Snippet._
import scala.util.Failure
import scala.util.Success
import scala.xml._
import net.shift._
import net.shift.engine.http._
import net.shift.template._
import net.shift.template.Snippet._
import net.shop.web.ShopApplication
import Binds._
import utils.ShopUtils._
import loc._
import java.util.Locale

object CategoryPage extends Cart[Request] { self =>

  val ? = Loc.loc0(new Locale("ro")) _

  def snippets = List(cartPopup, item)

  def reqSnip(name: String) = snip[Request](name) _

  val cartPopup = reqSnip("cart_popup") {
    s => (s.state, cartTemplate(s.state, s.state))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods =
          ShopApplication.productsService.allCategories() match {
            case Success(list) =>
              list flatMap { cat =>
                bind(s.node) {
                  case "li" > (a / childs) if (a hasClass "item") => <li>{ childs }</li>
                  case "a" > (attrs / childs) => <a id={ cat id } href={ "/products?cat=" + cat.id }>{ childs }</a>
                  case "div" > (a / childs) if (a hasClass "cat_box") => <div title={ cat title } style={ "background-image: url('" + categoryImagePath(cat) + "')" }>{ childs }</div> % a
                  case "div" > (a / _) if (a hasClass "info_tag_text") => <div>{ cat title }</div> % a
                }
              }

            case Failure(t) => <div class="error">{ ?("no_categories").text }</div>
          }

        (s.state, prods.toSeq)
      }
  }

}
