package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shift.common.Path
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.loc.Loc
import net.shift.template.Binds.bind
import net.shift.template.HasClass
import net.shift.template.HasId
import net.shift.template.PageState
import net.shop.api.ProductDetail
import net.shop.api.ShopError
import net.shop.api.persistence.NoSort
import net.shop.api.persistence.SortSpec
import net.shop.utils.ShopUtils.errorTag
import net.shop.utils.ShopUtils.imagePath
import net.shop.web.ShopApplication

object ProductsPage extends Cart[Request] {

  override def snippets = List(item, catList, prodListTemplate) ++ cartSnips

  val cartSnips = super.snippets

  val prodListTemplate = reqSnip("prod_list_template") {
    s =>
      Html5.runPageFromFile(s.state, Path(s"web/templates/productslist.html"), this).map { e => (e._1.state.initialState, e._2) }
  }

  def pageTitle(s: PageState[Request]) = {
    (s.initialState.param("cat"), s.initialState.param("search")) match {
      case (Some(cat :: _), None) =>
        ShopApplication.persistence.categoryById(cat) match {
          case Success(c) => c.title.getOrElse(s.lang.name, "???")
          case _          => ""
        }
      case (None, Some(search :: _)) => s""""$search""""
      case _                         => ""
    }
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ProductsQuery.fetch(s.state.initialState) match {
          case Success(list) =>
            list flatMap { prod =>
              bind(s.node) {
                case Xml("li", HasClass("item", a), childs)           => <li>{ childs }</li>
                case Xml("div", HasClass("item_box", a), childs)      => <div id={ prod stringId } title={ prod title_? (s.state.lang.name) } style={ "background: url('" + imagePath("normal", prod) + "') no-repeat" }>{ childs }</div> % a
                case Xml("div", HasClass("info_tag_text", a), childs) => <div>{ prod title_? (s.state.lang.name) }</div> % a
                case Xml("div", HasClass("info_tag_cart", a), childs) => if (prod.options.isEmpty && prod.userText.isEmpty)
                  Xml("div", a) / childs
                else
                  NodeSeq.Empty
                case Xml("div", HasClass("info_tag_price", a), childs) => priceTag(prod) % a
                case Xml("div", HasId("unique_ribbon", a), childs) => if (prod.unique)
                  <div class="unique_label" data-loc="unique"></div>
                else
                  NodeSeq.Empty
              } match {
                case Success(n) => n
                case Failure(f) => errorTag(f toString)
              }
            }
          case Failure(t) => errorTag(Loc.loc0(s.state.lang)("no.category").text)
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
        case Failure(ShopError(msg, _)) => Success((s.state.initialState, errorTag(Loc.loc0(s.state.lang)(msg).text)))
        case Failure(t)                 => Success((s.state.initialState, errorTag(Loc.loc0(s.state.lang)("no.category").text)))
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


