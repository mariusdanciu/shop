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
import net.shift.security.User
import net.shift.common.XmlUtils._
import net.shift.common.NodeOps._
import net.shift.engine.page.Html5
import net.shift.common.Path

object ProductsPage extends Cart[Request] with ShopUtils {

  override def snippets = List(title, item, catList, prodListTemplate) ++ cartSnips

  val cartSnips = super.snippets
  
  val prodListTemplate = reqSnip("prod_list_template") {
    s => 
       Html5.runPageFromFile(s.state, Path(s"web/templates/productslist.html"), this).map { e => (e._1.state.initialState, e._2)}
  }

  val title = reqSnip("title") {
    s =>
      val v = (s.state.initialState.param("cat"), s.state.initialState.param("search")) match {
        case (Some(cat :: _), None) =>
          ShopApplication.persistence.categoryById(cat) match {
            case Success(c) => Text(c.title.getOrElse(s.state.lang.name, "???"))
            case _          => NodeSeq.Empty
          }
        case (None, Some(search :: _)) => Text(s""""$search"""")
        case _                         => NodeSeq.Empty
      }
      Success((s.state.initialState, <h1>{ v }</h1>))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ProductsQuery.fetch(s.state.initialState) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case "li" attributes HasClass("item", a) / childs            => <li>{ childs }</li>
                case "div" attributes HasClass("item_box", a) / childs       => <div id={ prod stringId } title={ prod title_? (s.state.lang.name) } style={ "background: url('" + imagePath("normal", prod) + "') no-repeat" }>{ childs }</div> % a
                case "div" attributes HasClass("info_tag_text", a) / childs  => <div>{ prod title_? (s.state.lang.name) }</div> % a
                case "div" attributes HasClass("info_tag_price", a) / childs => priceTag(prod) % a
                case "div" attributes HasId("unique_ribbon", a) / childs => if (prod.unique)
                  <div class="unique_label" data-loc="unique"></div>
                else
                  NodeSeq.Empty
              } match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
          case Failure(t) => errorTag(Loc.loc0(s.state.lang)("no_category").text)
        }
        Success((s.state.initialState, prods.toSeq))
      }
  }

  val catList = reqSnip("catlist") {
    s =>
      ShopApplication.persistence.allCategories match {
        case Success(list) =>
          s.node match {
            case e: Elem =>
              val v = list.map(c => (<option value={ c.id getOrElse "?" }>{ c.title_?(s.state.lang.name) }</option>)).toSeq
              Success((s.state.initialState, e / NodeSeq.fromSeq(v)))
            case _ => Success((s.state.initialState, NodeSeq.Empty))
          }
        case Failure(t) => Success((s.state.initialState, errorTag(Loc.loc0(s.state.lang)("no_category").text)))
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
      case Some(v :: _) => SortSpec.fromString(v, r.language.name)
      case _            => NoSort
    }
  }
}


