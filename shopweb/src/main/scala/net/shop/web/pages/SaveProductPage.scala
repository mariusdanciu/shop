package net.shop.web.pages

import net.shift.http.HTTPRequest
import scala.xml.NodeSeq
import net.shop.web.services.ServiceDependencies
import scala.util.Success
import net.shift.common.XmlAttr
import scala.xml.Text
import scala.xml._
import net.shift.common.XmlUtils._
import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shop.api.ShopError
import net.shift.loc.Loc
import net.shift.template.SnipState
import net.shift.common.ShiftFailure
import scala.util.Failure
import scala.util.Try
import net.shop.api.ProductDetail
import net.shift.template.Snippet._
import net.shift.template.InlineState
import net.shop.utils.ShopUtils

trait SaveProductPage extends PageCommon[ProductPageState] with ServiceDependencies { self =>
  override def inlines = List(prod) ++ super.inlines
  override def snippets = List(catList) ++ super.snippets

  def product(s: InlineState[ProductPageState]): Try[ProductDetail] = {
    s.state.initialState.product match {
      case Failure(t) =>
        s.state.initialState.req.uri.paramValue("pid") match {
          case Some(id :: _) =>
            store.productById(id) match {
              case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
              case Failure(t)                 => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
              case s                          => s
            }
          case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
        }
      case s => s
    }
  }

  val prod = inline[ProductPageState]("prod") {
    s =>
      product(s) match {
        case Success(p) =>
          val out = s.params.head match {
            case "title"    => p.title_?(s.state.lang.name)
            case "price"    => ShopUtils.price(p.price)
            case "discount" => p.discountPrice map { ShopUtils.price } getOrElse ""
            case "keywords" => p.keyWords mkString ","
            case "stock"    => p.stock getOrElse "" toString
            case "pos"      => p.position getOrElse "" toString
            case "unique"   => p.unique.toString()
            case "specs"    => p.properties map { case (k, v) => s"$k=$v" } mkString "\n"
            case "desc"     => p.description_?(s.state.lang.name)
          }
          Success((s.state.initialState.copy(product = Success(p)), out))
        case f =>
          Success((s.state.initialState, ""))
      }

  }

  val catList = reqSnip("catlist") {
    s =>
      val prodCats = s.state.initialState.product map { p => p.categories } getOrElse Nil

      store.allCategories match {
        case Success(cats) =>
          val res = NodeSeq.fromSeq((for { c <- cats } yield {
            if (prodCats.contains(c.stringId)) {
              <option value="c.stringId" selected="true">{ Text(c.title_?(s.state.initialState.req.language.name)) }</option>
            } else
              Xml("option", XmlAttr("value", c.stringId)) / Text(c.title_?(s.state.initialState.req.language.name))
          }).toSeq)
          Success((s.state.initialState, res))
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }
  }
}
