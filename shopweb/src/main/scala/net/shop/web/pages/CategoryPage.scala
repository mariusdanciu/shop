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

object CategoryPage extends DynamicContent[Request] {

  def snippets = List(item)

  def reqSnip(name: String) = snip[Request](name) _

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
                  case "div" > (a / childs) if (a hasClass "item_box") => <div title={ cat title } style={ "background-image: url('" + categoryImagePath(cat) + "')" }>{ childs }</div>
                  case "div" > (a / _) if (a hasClass "price_tag_text") => <div>{ cat title }</div> % a
                }
              }

            case Failure(t) => <p>error</p>
          }

        (s.state, prods.toSeq)
      }
  }

}
