package net.shop
package web.pages

import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml._
import net.shift.engine._
import net.shift.engine.http.Request._
import net.shift.engine.http.Request
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip
import net.shop.web.ShopApplication
import net.shop.utils.ShopUtils._
import scalax.io.JavaConverters
import scalax.io.Resource
import net.shift.common.XmlUtils
import net.shift.common.Path
import net.shop.model.ProductDetail

object ProductDetailPage extends Cart[ProductPageState] {

  override def snippets = List(cartPopup, title, catlink, images, detailPrice, details) ++ super.snippets

  def reqSnip(name: String) = snip[ProductPageState](name) _

  val cartPopup = reqSnip("cart_popup") {
    s => cartTemplate(s.state, s.state.req) map { (s.state, _) }
  }

  val title = reqSnip("title") {
    s =>
      val v = s.state.req.param("pid") match {
        case id :: _ => ShopApplication.productsService.productById(id) match {
          case Success(prod) => (ProductPageState(s.state.req, Some(prod)), <h1>{ prod.title }</h1>)
          case Failure(t) => (ProductPageState(s.state.req, None), NodeSeq.Empty)
        }
        case Nil => (ProductPageState(s.state.req, None), NodeSeq.Empty)
      }

      bind(s.node) {
        case "span" > (_ / childs) => v._2
      } map { (v._1, _) }
  }

  val catlink = reqSnip("catlink") {
    s =>
      (s.state.product match {
        case Success(p) =>
          Try(p.categories.flatMap(e => {
            ShopApplication.productsService.categoryById(e) match {
              case Success(s) => (<a href={ s"/products?cat=${e}" }>{ s.title }</a> ++ <span>, </span>)
              case _ => NodeSeq.Empty
            }
          }).toList.dropRight(1))
        case _ => Success(NodeSeq.Empty)
      }) map { (s.state, _) }
  }

  val images = reqSnip("images") {
    s =>
      s.state.product match {
        case Success(prod) =>
          bind(s.node) {
            case "b:img" > _ => <img class="sel_img" src={ augmentImagePath(prod.id, prod.images.head) } title={ prod.title }></img>
            case "f:li" > (_ / childs) => childs
            case "f:img" > (a / childs) =>
              prod.images map { i =>
                <li><a class="small_img" href="#">{ <img src={ augmentImagePath(prod.id, i) } title={ prod.title }/> % a }</a></li>
              }
          } map { b => (ProductPageState(s.state.req, Some(prod)), b) }
        case _ => Success((ProductPageState(s.state.req, None), errorTag(Loc.loc0(s.state.req.language)("no_product").text)))
      }
  }

  val detailPrice = reqSnip("detailPrice") {
    s =>
      (for {
        p <- s.state.product
      } yield {
        (ProductPageState(s.state.req, Some(p)), Text(s"${p.price} RON"))
      })
  }

  val details = reqSnip("details") {
    s =>
      import JavaConverters.asInputConverter
      (for {
        p <- s.state.product
        input <- s.state.req.resource(Path(s"data/products/${p.id}/desc.html"))
        n <- load(input)
      } yield {
        (ProductPageState(s.state.req, Some(p)), n)
      })
  }

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, None)
}

case class ProductPageState(req: Request, product: Try[ProductDetail])
