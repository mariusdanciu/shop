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
import net.shift.common.State
import net.shop.backend.SortSpec
import net.shop.backend.NoSort
import net.shop.backend.SortByName
import net.shop.backend.SortByPrice
import net.shop.utils.ShopUtils

object ProductsPage extends Cart[Request] with ShopUtils {

  override def snippets = List(title, item) ++ cartSnips

  val cartSnips = super.snippets

  val title = reqSnip("title") {
    s =>
      val v = (s.state.param("cat"), s.state.param("search")) match {
        case (Some(cat :: _), None) =>
          ShopApplication.productsService.categoryById(cat) match {
            case Success(c) => Text(c.title.getOrElse(s.language.language, "???"))
            case _ => NodeSeq.Empty
          }
        case (None, Some(search :: _)) => Text(s""""$search"""")
        case _ => NodeSeq.Empty
      }
      Success((s.state, <h1>{ v }</h1>))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ProductsQuery.fetch (s.state) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case "li" :/ HasClass("item", a) / childs => <li>{ childs }</li>
                case "div" :/ HasClass("item_box", a) / childs => <div id={ prod id } title={ prod title_? (s.language) } style={ "background-image: url('" + imagePath(prod) + "')" }>{ childs }</div> % a
                case "div" :/ HasClass("info_tag_text", a) / childs => <div>{ prod title_? (s.language) }</div> % a
                case "div" :/ HasClass("info_tag_price", a) / childs => priceTag(prod) % a
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

}

object ProductsQuery {
  def fetch(r: Request): Try[Traversable[ProductDetail]] = {
    lazy val spec = toSortSpec(r)
    (r.param("cat"), r.param("search")) match {
      case (Some(cat :: _), None) => ShopApplication.productsService.categoryProducts(cat, spec)
      case (None, Some(search :: _)) => ShopApplication.productsService.searchProducts(search, spec)
      case _ => Success(Nil)
    }
  }

  def toSortSpec(r: Request): SortSpec = {
    r.param("sort") match {
      case Some(v :: _) => SortSpec.fromString(v, r.language)
      case _ => NoSort
    }
  }
}
