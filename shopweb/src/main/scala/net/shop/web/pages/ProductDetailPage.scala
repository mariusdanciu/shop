package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.xml._
import scalax.io._
import net.shift.engine.http.Request
import net.shift.common._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip
import net.shop.backend.ProductDetail
import net.shop.web.ShopApplication
import utils.ShopUtils._
import java.io.StringReader
import net.shift.loc.Loc
import java.util.Locale

object ProductDetailPage extends DynamicContent[ProductPageState] {

  val ? = Loc.loc0(new Locale("ro")) _

  def snippets = List(title, images, details)

  def reqSnip(name: String) = snip[ProductPageState](name) _

  val title = reqSnip("title") {
    s =>
      s.state.req.param("pid") match {
        case id :: _ => ShopApplication.productsService.productById(id) match {
          case Success(prod) => (ProductPageState(s.state.req, Some(prod)), bind(s.node) {
            case "span" > (_ / childs) => <h1>{ prod.title }</h1>
          });
          case Failure(t) => (ProductPageState(s.state.req, None), <span>{?("no_product")}</span>);
        }
        case Nil => (ProductPageState(s.state.req, None), <span>{?("no_product")}</span>)
      }
  }

  val images = reqSnip("images") {
    s =>
      s.state.product match {
        case Some(prod) =>
          (ProductPageState(s.state.req, Some(prod)), bind(s.node) {
            case "b:img" > _ => <img src={ augmentImagePath(prod.id, prod.images.head) } title={ prod.title }></img>
            case "f:li" > (_ / childs) => childs
            case "f:img" > (a / childs) =>
              prod.images map { i =>
                <li>{ <img src={ augmentImagePath(prod.id, i) } title={ prod.title }/> % a }</li>
              }
          })
        case _ => (ProductPageState(s.state.req, None), NodeSeq.Empty);
      }
  }

  val details = reqSnip("details") {
    s =>
      import JavaConverters.asInputConverter
      (for {
        p <- s.state.product
      } yield {
        (ProductPageState(s.state.req, Some(p)), XmlUtils.load(Resource.fromFile(s"data/products/${p.id}/desc.html")))
      }) getOrElse
        (ProductPageState(s.state.req, None), NodeSeq.Empty)
  }

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, None)
}

case class ProductPageState(req: Request, product: Option[ProductDetail])
