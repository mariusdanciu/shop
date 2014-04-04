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

  override def snippets = List(title, item) ++ super.snippets

  val title = reqSnip("title") {
    s =>
      val v = (s.state.param("cat"), s.state.param("search")) match {
        case (cat :: _, Nil) =>
          ShopApplication.productsService.categoryById(cat) match {
            case Success(c) => Text(c.title.getOrElse(s.language.language, "???"))
            case _ => NodeSeq.Empty
          }
        case (Nil, search :: _) => Text(s""""$search"""")
        case _ => NodeSeq.Empty
      }
      Success((s.state, <h1>{ v }</h1>))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = fetch(s.state) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case "li" :/ HasClass("item", a) / childs => <li>{ childs }</li>
                case "div" :/ HasClass("item_box", a) / childs => <div id={ prod id } title={ prod title_? (s.language) } style={ "background-image: url('" + imagePath(prod) + "')" }>{ childs }</div> % a
                case "div" :/ HasClass("info_tag_text", a) / childs => <div>{ prod title_? (s.language) }</div> % a
                case "div" :/ HasClass("info_tag_price", a) / childs => <div>{ s"${prod.price.toString} RON" }</div> % a
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
