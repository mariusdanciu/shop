package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml._
import net.shift.common.Path
import net.shift.common.XmlUtils
import net.shift.engine._
import net.shift.engine.http.Request._
import net.shift.engine.http.Request
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip
import net.shop.model.ProductDetail
import net.shop.utils.ShopUtils
import net.shop.web.ShopApplication
import scalax.io.JavaConverters

object ProductDetailPage extends Cart[ProductPageState] with ShopUtils {

  override def snippets = List(title, catlink, productLink, images, detailPrice, details) ++ super.snippets

  val title = reqSnip("title") {
    s =>
      val v = s.state.req.param("pid") match {
        case Some(id :: _) => ShopApplication.persistence.productById(id) match {
          case Success(prod) => (ProductPageState(s.state.req, Success(prod)), <h1>{ prod.title_?(s.language) }</h1>)
          case Failure(t) =>
            t printStackTrace ()
            (ProductPageState.build(s.state.req), NodeSeq.Empty)
        }
        case _ => (ProductPageState.build(s.state.req), NodeSeq.Empty)
      }
      Success(v)
  }

  val catlink = reqSnip("catlink") {
    s =>
      (s.state.product match {
        case Success(p) =>
          Try(p.categories.flatMap(e => {
            ShopApplication.persistence.categoryById(e) match {
              case Success(cat) => (<a href={ s"/products?cat=${e}" }>{ cat.title_?(s.language) }</a> ++ <span>, </span>)
              case _ => NodeSeq.Empty
            }
          }).toList.dropRight(1))
        case _ => Success(NodeSeq.Empty)
      }) map { (s.state, _) }
  }

  val productLink = reqSnip("productlink") {
    s =>
      (s.state.product match {
        case Success(p) =>
          for {
            prod <- s.state.product
            el <- bind(s.node) {
              case "a" :/ _ => <a href={ s"/product?pid=${prod.id}" }>{ Loc.loc0(s.language)("product.page").text }</a>
            }
          } yield {
            el
          }

        case _ => Success(<p>Not found {s.state.product}</p>)
      }) map { (s.state, _) }
  }

  val images = reqSnip("images") {
    s =>
      s.state.product match {
        case Success(prod) =>
          bind(s.node) {
            case "b:img" :/ _ =>
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
        (ProductPageState(s.state.req, Success(p)), priceTag(p))
      })
  }

  val details = reqSnip("details") {
    s =>
      import JavaConverters.asInputConverter
      (for {
        p <- s.state.product
        input <- s.state.req.resource(Path(s"data/products/${p.id}/desc_${s.language}.html"))
        n <- load(input)
      } yield {
        (ProductPageState(s.state.req, Success(p)), n)
      })
  }

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, Failure[ProductDetail](new RuntimeException("Product not found")))
}

case class ProductPageState(req: Request, product: Try[ProductDetail]) 
