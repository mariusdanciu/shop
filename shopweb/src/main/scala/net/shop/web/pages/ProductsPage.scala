package net.shop
package web.pages

import java.util.Locale

import scala.util.Failure
import scala.util.Success
import scala.xml._
import scala.xml._

import net.shift._
import net.shift._
import net.shift.engine.http._
import net.shift.engine.http._
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Snippet._
import net.shop.web.ShopApplication
import utils.ShopUtils._

object ProductsPage extends DynamicContent[Request] {
  val ? = Loc.loc0(new Locale("ro")) _

  def snippets = List(title, item)

  def reqSnip(name: String) = snip[Request](name) _

  val title = reqSnip("title") {
    s =>
      val v = s.state.param("cat") match {
        case cat :: _ =>
          ShopApplication.productsService.categoryById(cat) match {
            case Success(c) => Text(c.title)
            case _ => NodeSeq.Empty
          }

        case Nil => NodeSeq.Empty
      }

      (s.state, bind(s.node) {
        case "span" > (_ / childs) => <h1>{ v }</h1>
      })
  }
  val item = reqSnip("item") {
    s =>
      {
        val prods =
          s.state.param("cat").flatMap { cat =>
            ShopApplication.productsService.categoryProducts(cat) match {
              case Success(list) =>
                list flatMap { prod =>
                  bind(s.node) {
                    case "li" > (a / childs) if (a hasClass "item") => <li>{ childs }</li>
                    case "a" > (attrs / childs) => <a id={ prod id } href={ s"/product?pid=${prod.id}" }>{ childs }</a>
                    case "div" > (a / childs) if (a hasClass "item_box") => <div title={ prod title } style={ "background-image: url('" + productImagePath(prod) + "')" }>{ childs }</div>
                    case "div" > (a / _) if (a hasClass "info_tag_text") => <div>{ prod title }</div> % a
                    case "div" > (a / _) if (a hasClass "info_tag_price") => <div>{ s"${prod.price.toString} RON" }</div> % a
                  }
                }

              case Failure(t) => errorTag(?("no_category").text)
            }
          }
        (s.state, prods.toSeq)
      }
  }

}
