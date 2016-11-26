package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import net.shift.common.Path
import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shift.engine.page.Html5
import net.shift.loc.Loc
import net.shift.template.Binds.bind
import net.shift.template.HasClass
import net.shift.template.HasId
import net.shift.template.PageState
import net.shift.template.SnipState
import net.shop.api.ProductDetail
import net.shop.api.ShopError
import net.shop.api.persistence.NoSort
import net.shop.api.persistence.SortSpec
import net.shop.utils.ShopUtils.errorTag
import net.shop.utils.ShopUtils.imagePath
import net.shop.web.ShopApplication
import net.shop.web.services.ServiceDependencies
import net.shop.api.persistence.Persistence
import scala.xml.Text
import net.shift.template.HasClasses
import net.shift.server.http.Request
import net.shift.common.XmlAttr
import net.shift.common.ShiftFailure

object ProductsPage {

  def extractCat(r: Request): Try[String] = {
    Path(r.uri.path) match {
      case Path(_, _ :: _ :: id :: _) => Success(id)
      case _                          => ShiftFailure("Category was not set").toTry
    }
  }
}
trait ProductsPage extends PageCommon[Request] with ServiceDependencies {

  override def snippets = List(catName, item, catList, sort) ++ cartSnips

  val cartSnips = super.snippets

  private def render(s: SnipState[Request], prod: ProductDetail): NodeSeq = {
    bind(s.node) {
      case Xml("figure", a, c) => Xml("figure", XmlAttr(Map("id" -> prod.stringId))) / c
      case Xml("a", HasClasses(_ :: "add_to_cart_box" :: _, a), childs) => <a id={ prod.stringId }>{ childs }</a> % a
      case Xml("img", a, _) =>
        <img title={ prod title_? (s.state.lang.name) } src={ imagePath("normal", prod) }></img> % a
      case Xml("h3", a, _) =>
        <h3>{ prod title_? (s.state.lang.name) }</h3> % a
      case Xml("p", a, childs) => <p>{ priceTag(prod) % a }</p>
      case Xml("div", HasClass("unicat", a), childs) => if (prod.unique)
        <div></div> % a
      else
        NodeSeq.Empty
    } match {
      case Success(n) => n
      case Failure(f) => errorTag(f toString)
    }
  }

  val catName = reqSnip("cat_name") {
    s =>

      val c = (for {
        id <- ProductsPage.extractCat(s.state.initialState).toOption
        cat <- store.categoryById(id).toOption
      } yield {
        Text(cat.title_?(s.state.lang.name))
      }) orElse {
        s.state.initialState.r.uri.param("search") map { s => Text(s.value.head) }
      } getOrElse {
        errorTag(Loc.loc0(s.state.lang)("no.category").text)
      }

      Success((s.state.initialState, c))
  }

  val item = reqSnip("item") {
    s =>
      {
        val prods = ProductsQuery.fetch(s.state.initialState, store) match {
          case Success(list) =>

            val (nopos, pos) = list.span(p => p.position.isEmpty)

            val nodes = (pos flatMap { (p: ProductDetail) => render(s, p) }) ++
              (nopos flatMap { (p: ProductDetail) => render(s, p) })

            nodes.grouped(4).map { l => <div class="row hover01">{ NodeSeq.fromSeq(l) }</div> }
          case Failure(t) =>
            errorTag(Loc.loc0(s.state.lang)("no.category").text)
        }
        Success((s.state.initialState, prods.toSeq))
      }
  }

  val catList = reqSnip("catlist") {
    s =>
      store.allCategories match {
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

  val sort = reqSnip("sort") {
    s =>
      val n = s.state.initialState.uri.paramValue("sort") match {
        case Some(v :: _) => bind(s.node) {
          case Xml("option", attrs, childs) if (attrs.attrs.get("value") == Some(v)) =>
            Xml("option", XmlAttr(attrs.attrs + ("selected" -> "true")), childs)
          case n => n
        } getOrElse s.node
        case _ => s.node
      }

      Success((s.state.initialState, n))
  }

}

object ProductsQuery {
  def fetch(r: Request, store: Persistence): Try[Iterator[ProductDetail]] = {
    lazy val spec = toSortSpec(r)
    (ProductsPage.extractCat(r), r.uri.paramValue("search")) match {
      case (Success(cat), None)   => store.categoryProducts(cat, spec)
      case (_, Some(search :: _)) => store.searchProducts(search, spec)
      case _                      => Success(Iterator.empty)
    }
  }

  def toSortSpec(r: Request): SortSpec = {
    r.uri.paramValue("sort") match {
      case Some(v :: _) => SortSpec.fromString(v, r.language.name)
      case _            => NoSort
    }
  }
}


