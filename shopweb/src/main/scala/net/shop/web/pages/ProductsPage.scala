package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
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
import net.shop.model.ProductDetail
import net.shop.web.ShopApplication
import utils.ShopUtils._

object ProductsPage extends Cart[Request] {

  override def snippets = List(cartPopup, title, item) ++ super.snippets

  val title = reqSnip("title") {
    s =>
      val v = (s.state.param("cat"), s.state.param("search")) match {
        case (cat :: _, Nil) =>
          ShopApplication.productsService.categoryById(cat) match {
            case Success(c) => Text(c.title.getOrElse(s.language.language, "???"))
            case _ => NodeSeq.Empty
          }
        case (Nil, search :: _) => s""""$search""""
        case _ => NodeSeq.Empty
      }

      bind(s.node) {
        case "span" > (_ / childs) => <h1>{ v }</h1>
      } map { (s.state, _) }
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = fetch(s.state) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case "li" > (a / childs) if (a hasClass "item") => <li>{ childs }</li>
                case "a" > (attrs / childs) => <a id={ prod id } href={ s"/product?pid=${prod.id}" }>{ childs }</a>
                case "div" > (a / childs) if (a hasClass "item_box") => <div title={ prod title_? (s.language) } style={ "background-image: url('" + imagePath(prod) + "')" }>{ childs }</div> % a
                case "div" > (a / _) if (a hasClass "info_tag_text") => <div>{ prod title_? (s.language) }</div> % a
                case "div" > (a / _) if (a hasClass "info_tag_price") => <div>{ s"${prod.price.toString} RON" }</div> % a
              } match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
          case Failure(t) => errorTag(Loc.loc0(s.language)("no_category").text)
        }
        Success((s.state, prods.toSeq))
      }
  }

  def fetch(r: Request): Try[Traversable[ProductDetail]] = {
    (r.param("cat"), r.param("search")) match {
      case (cat :: _, Nil) => ShopApplication.productsService.categoryProducts(cat)
      case (Nil, search :: _) => ShopApplication.productsService.searchProducts(search)
      case _ => Success(Nil)
    }
  }

}
