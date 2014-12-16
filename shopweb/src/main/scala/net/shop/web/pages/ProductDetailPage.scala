package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml._
import net.shift.common.XmlUtils
import net.shift.engine._
import net.shift.engine.http.Request._
import net.shift.engine.http.Request
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip
import net.shop.api.ProductDetail
import net.shop.utils.ShopUtils
import net.shop.web.ShopApplication
import net.shift.common.NodeOps._
import net.shift.loc.Language

object ProductDetailPage extends Cart[ProductPageState] with ShopUtils with XmlUtils {

  override def snippets = List(title, catlink, productLink, images, detailPrice, details, specs, edit) ++ super.snippets

  val title = reqSnip("title") {
    s =>
      {
        s.state.req.param("pid") match {
          case Some(id :: _) => ShopApplication.persistence.productById(id) match {
            case Success(prod) => Success(ProductPageState(s.state.req, Success(prod)), <h1>{ prod.title_?(s.language.language) }</h1>)
            case Failure(t) =>
              Success(s.state, errorTag(Loc.loc0(s.language)("no_product").text))
          }
          case _ => Success(s.state, errorTag(Loc.loc0(s.language)("no_product").text))
        }
      }
  }

  val catlink = reqSnip("catlink") {
    s =>
      ((s.state.product map { p =>
        (p.categories.flatMap(e => {
          ShopApplication.persistence.categoryById(e) match {
            case Success(cat) => (<a href={ s"/products?cat=${e}" }>{ cat.title_?(s.language.language) }</a> ++ <span>, </span>)
            case _            => NodeSeq.Empty
          }
        }).toList.dropRight(1))
      }) map { l => (s.state, NodeSeq.fromSeq(l)) }).recover { case _ => (s.state, NodeSeq.Empty) }
  }

  val productLink = reqSnip("productlink") {
    s =>
      ((s.state.product flatMap { p =>
        for {
          prod <- s.state.product
          el <- bind(s.node) {
            case "a" - _ => <a href={ s"/product?pid=${prod.stringId}" }>{ Loc.loc0(s.language)("product.page").text }</a>
          }
        } yield {
          el
        }
      }) map { (s.state, _) }).recover { case _ => (s.state, NodeSeq.Empty) }
  }

  val images = reqSnip("images") {
    s =>
      (s.state.product flatMap { prod =>
        bind(s.node) {
          case "b:img" - _ =>
            val list = NodeSeq.fromSeq(for {
              p <- prod.images zipWithIndex
            } yield {
              val normal = imagePath(prod.stringId, "normal", p._1)
              val large = imagePath(prod.stringId, "large", p._1)
              val thumb = imagePath(prod.stringId, "thumb", p._1)
              <a href="#" data-image={ normal } data-zoom-image={ large }>
                <img id={ s"img_${p._2}" } src={ thumb }/>
              </a>
            })

            val path = imagePath(prod.stringId, "normal", prod.images.head)
            val large = imagePath(prod.stringId, "large", prod.images.head)

            <img id="sel_img" src={ path } title={ prod.title_?(s.language.language) } data-zoom-image={ large }></img> ++
              <div id="gallery">
                { list }
              </div>

        } map { b => (ProductPageState(s.state.req, Success(prod)), b) }
      }).recover { case _ => (s.state, NodeSeq.Empty) }
  }

  val detailPrice = reqSnip("detailPrice") {
    s =>
      (for {
        p <- s.state.product
      } yield {
        (ProductPageState(s.state.req, Success(p)), priceTag(p))
      }).recover { case _ => (s.state, NodeSeq.Empty) }
  }

  private def option2Try[T](o: Option[T]): Try[T] = o match {
    case Some(v) => Success(v)
    case _       => ShiftFailure("Empty")
  }

  val details = reqSnip("details") {
    s =>
      {
        (for {
          p <- s.state.product
          desc <- option2Try(p.description.get(s.language.language))
        } yield {
          (ProductPageState(s.state.req, Success(p)), Text(desc))
        }).recover { case _ => (s.state, NodeSeq.Empty) }
      }
  }

  val specs = reqSnip("specs") {
    s =>
      {
        (for {
          p <- s.state.product
        } yield {
          val n = (NodeSeq.Empty /: p.properties) {
            case (acc, (k, v)) =>
              (bind(s.node) {
                case HasClass("prop_name", a)  => Text(k)
                case HasClass("prop_value", a) => Text(v)
              }) match {
                case Success(n) => acc ++ n
                case _          => NodeSeq.Empty
              }
          }
          (ProductPageState(s.state.req, Success(p)), n)
        }).recover { case _ => (s.state, NodeSeq.Empty) }
      }
  }

  val edit = reqSnip("edit") {
    s =>
      {
        (for {
          p <- s.state.product
        } yield {
          val title = p.title.get(s.language.language).getOrElse("")
          val desc = p.description.get(s.language.language).getOrElse("")
          val oldPrice = p.oldPrice.map(_.toString()).getOrElse("")

          (bind(s.node) {
            case "form" - attrs / childs                           => node("form", attrs.attrs + ("action" -> ("/product/update/" + p.stringId))) / childs
            case HasId("edit_pid", attrs)                          => node("input", attrs.attrs + ("value" -> p.stringId))
            case HasId("edit_title", attrs)                        => node("input", attrs.attrs + ("value" -> title))
            case HasId("edit_price", attrs)                        => node("input", attrs.attrs + ("value" -> p.price.toString()))
            case HasId("edit_discount_price", attrs)               => node("input", attrs.attrs + ("value" -> oldPrice))
            case HasId("edit_categories", attrs)                   => handleCategories(attrs, s.language, p.categories.toSet)
            case HasId("edit_keywords", attrs)                     => node("input", attrs.attrs + ("value" -> p.keyWords.mkString(", ")))
            case HasId("edit_description", attrs)                  => node("textarea", attrs.attrs) / Text(desc)
            case _ - HasClass("edit_props_sample", attrs) / childs => handleProperties(childs, p)
          }) match {
            case Success(n) => (ProductPageState(s.state.req, Success(p)), n)
            case _          => (ProductPageState(s.state.req, Success(p)), s.node)
          }
        }).recover { case _ => (s.state, NodeSeq.Empty) }
      }
  }

  private def handleCategories(attrs: Attributes, l: Language, categs: Set[String]) = node("select", attrs.attrs) / {
    ShopApplication.persistence.allCategories match {
      case Success(cats) => NodeSeq.fromSeq((for { c <- cats } yield {
        val opt = node("option", Map("value" -> c.stringId)) / Text(c.title_?(l.language))
        if (categs.contains(c.stringId)) {
          (opt attr ("selected", "true")).e
        } else {
          opt.e
        }
      }).toSeq)
      case _ => node("select", attrs.attrs)
    }
  }

  private def handleProperties(childs: NodeSeq, p: ProductDetail) = NodeSeq.fromSeq((p.properties flatMap {
    case (k, v) =>
      bind(node("div", Map("class" -> "row")) / childs) {
        case "k:input" - attrs / _ =>
          node("input", attrs.attrs + ("value" -> k))
        case "v:input" - attrs / _ =>
          node("input", attrs.attrs + ("value" -> v))
      } match {
        case Success(n) => n
        case _          => NodeSeq.Empty
      }
  }).toSeq)

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, Failure[ProductDetail](new RuntimeException("Product not found")))
}

case class ProductPageState(req: Request, product: Try[ProductDetail]) 

