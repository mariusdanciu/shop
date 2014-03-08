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

  override def snippets = List(title, catlink, images, detailPrice, details) ++ super.snippets

  val title = reqSnip("title") {
    s =>
      val v = s.state.req.param("pid") match {
        case id :: _ => ShopApplication.productsService.productById(id) match {
          case Success(prod) => (ProductPageState(s.state.req, Success(prod)), <h1>{ prod.title_?(s.language) }</h1>)
          case Failure(t) => (ProductPageState.build(s.state.req), NodeSeq.Empty)
        }
        case Nil => (ProductPageState.build(s.state.req), NodeSeq.Empty)
      }
      val k = bind(s.node) {
        case "span" > (_ / childs) => v._2
      } map { (v._1, _) }
      
      k
  }

  val catlink = reqSnip("catlink") {
    s =>
      (s.state.product match {
        case Success(p) =>
          Try(p.categories.flatMap(e => {
            ShopApplication.productsService.categoryById(e) match {
              case Success(cat) => (<a href={ s"/products?cat=${e}" }>{ cat.title_?(s.language) }</a> ++ <span>, </span>)
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
            case "b:img" > _ =>
              val list = NodeSeq.fromSeq(for {
                p <- prod.images zipWithIndex
              } yield {
                val normal = imagePath(prod.id, p._1)
                val large = imagePath(prod.id, "large", p._1)
                val thumb = imagePath(prod.id, "thumb", p._1)
                <a href="#" data-image={ normal } data-zoom-image={ large }>
                  <img id={ s"img_${p._2}" } src={ thumb }/>
                </a>
              })

              val path = imagePath(prod.id, prod.images.head)
              val large = imagePath(prod.id, "large", prod.images.head)

              <img id="sel_img" src={ path } title={ prod.title_?(s.language) } data-zoom-image={ large }></img> ++
                <div id="gallery">
                  { list }
                </div>

          } map { b => (ProductPageState(s.state.req, Success(prod)), b) }
        case _ => Success((ProductPageState.build(s.state.req), errorTag(Loc.loc0(s.state.req.language)("no_product").text)))
      }
  }

  val detailPrice = reqSnip("detailPrice") {
    s =>
      (for {
        p <- s.state.product
      } yield {
        (ProductPageState(s.state.req, Success(p)), Text(s"${p.price} RON"))
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
        (ProductPageState(s.state.req, Success(p)), n)
      })
  }

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, ShiftFailure[ProductDetail])
}

case class ProductPageState(req: Request, product: Try[ProductDetail]) 
