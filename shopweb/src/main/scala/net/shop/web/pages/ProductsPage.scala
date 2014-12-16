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
import net.shop.api.ProductDetail
import net.shop.web.ShopApplication
import net.shift.common.State
import net.shop.api.persistence.SortSpec
import net.shop.api.persistence.NoSort
import net.shop.api.persistence.SortByName
import net.shop.api.persistence.SortByPrice
import net.shop.utils.ShopUtils

object ProductsPage extends Cart[Request] with ShopUtils {

  override def snippets = List(title, item, itemAdd, catList) ++ cartSnips

  val cartSnips = super.snippets

  val title = reqSnip("title") {
    s =>
      val v = (s.state.param("cat"), s.state.param("search")) match {
        case (Some(cat :: _), None) =>
          ShopApplication.persistence.categoryById(cat) match {
            case Success(c) => Text(c.title.getOrElse(s.language.language, "???"))
            case _          => NodeSeq.Empty
          }
        case (None, Some(search :: _)) => Text(s""""$search"""")
        case _                         => NodeSeq.Empty
      }
      Success((s.state, <h1>{ v }</h1>))
  }

  val itemAdd = reqSnip("itemadd") {
    s => Success((s.state, s.node))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ProductsQuery.fetch(s.state) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case "li" - HasClass("item", a) / childs            => <li>{ childs }</li>
                case "div" - HasClass("item_box", a) / childs       => <div id={ prod stringId } title={ prod title_? (s.language.language) } style={ "background-image: url('" + imagePath("normal", prod) + "')" }>{ childs }</div> % a
                case "div" - HasClass("info_tag_text", a) / childs  => <div>{ prod title_? (s.language.language) }</div> % a
                case "div" - HasClass("info_tag_price", a) / childs => priceTag(prod) % a
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

  val catList = reqSnip("catlist") {
    s =>
      ShopApplication.persistence.allCategories match {
        case Success(list) =>
          s.node match {
            case e: Elem =>
              val v = list.map(c => (<option value={ c.id getOrElse "?" }>{ c.title_?(s.language.language) }</option>)).toSeq
              Success((s.state, e / NodeSeq.fromSeq(v)))
            case _ => Success((s.state, NodeSeq.Empty))
          }
        case Failure(t) => Success((s.state, errorTag(Loc.loc0(s.language)("no_category").text)))
      }
  }

}

object ProductsQuery {
  def fetch(r: Request): Try[Iterator[ProductDetail]] = {
    lazy val spec = toSortSpec(r)
    (r.param("cat"), r.param("search")) match {
      case (Some(cat :: _), None)    => ShopApplication.persistence.categoryProducts(cat, spec)
      case (None, Some(search :: _)) => ShopApplication.persistence.searchProducts(search, spec)
      case _                         => Success(Iterator.empty)
    }
  }

  def toSortSpec(r: Request): SortSpec = {
    r.param("sort") match {
      case Some(v :: _) => SortSpec.fromString(v, r.language.language)
      case _            => NoSort
    }
  }
}
