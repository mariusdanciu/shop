package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml._
import net.shift.common.XmlUtils._
import net.shift.engine._
import net.shift.engine.http.Request._
import net.shift.engine.http.Request
import net.shift.loc.Loc
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip
import net.shop.api.ProductDetail
import net.shop.utils.ShopUtils._
import net.shop.web.ShopApplication
import net.shift.loc.Language
import net.shift.common.ShiftFailure
import net.shift.security.User
import net.shift.common.Config
import net.shop.api.ShopError
import net.shift.common.Attributes
import net.shift.common.BNode
import net.shift.common.BNodeImplicits._


object ProductDetailPage extends Cart[ProductPageState] {

  override def snippets = List(meta, catlink, productLink, images, detailPrice, stock, details, specs, customize, edit) ++ super.snippets

  val meta = reqSnip("fb_meta") {
    s =>
      {
        s.state.initialState.req.param("pid") match {
          case Some(id :: _) => ShopApplication.persistence.productById(id) match {
            case Success(prod) =>
              val fb = bind(s.node) {
                case BNode("meta",  a, _) if (a.hasAttr(("property", "og:url")))               => BNode("meta", a + ("content",  s"http://${Config.string("host")}/product?pid=${prod.stringId}")).toElem
                case BNode("meta",  a, _) if (a.hasAttr(("property", "og:title")))             => BNode("meta", a + ("content",  prod.title_?(s.state.lang.name))).toElem
                case BNode("meta",  a, _) if (a.hasAttr(("property", "og:description")))       => BNode("meta", a + ("content",  prod.title_?(s.state.lang.name))).toElem
                case BNode("meta",  a, _) if (a.hasAttr(("property", "og:image")))             => BNode("meta", a + ("content",  s"http://${Config.string("host")}${imagePath(prod.stringId, "normal", prod.images.head)}")).toElem
                case BNode("meta",  a, _) if (a.hasAttr(("property", "product:price:amount"))) => BNode("meta", a + ("content",  price(prod.price))).toElem
              }
              for { n <- fb } yield {
                (ProductPageState(s.state.initialState.req, Success(prod), s.state.user), n)
              }
            case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
            case Failure(t) =>
              ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
          }
          case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
        }
      }
  }
  def pageTitle(s: PageState[ProductPageState]) =
    s.initialState.product match {
      case Success(prod) => prod.title_?(s.lang.name)
      case Failure(t)    => ""
    }

  val catlink = reqSnip("catlink") {
    s =>
      ((s.state.initialState.product map { p =>
        (p.categories.flatMap(e => {
          ShopApplication.persistence.categoryById(e) match {
            case Success(cat) => (<a href={ s"/products?cat=${e}" }>{ cat.title_?(s.state.lang.name) }</a> ++ <span>, </span>)
            case _            => NodeSeq.Empty
          }
        }).toList.dropRight(1))
      }) map { l => (s.state.initialState, NodeSeq.fromSeq(l)) }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  val productLink = reqSnip("productlink") {
    s =>
      ((s.state.initialState.product flatMap { p =>
        for {
          prod <- s.state.initialState.product
          el <- bind(s.node) {
            case BNode("a", _, _) => <a href={ s"/product?pid=${prod.stringId}" }>{ Loc.loc0(s.state.lang)("product.page").text }</a>
          }
        } yield {
          el
        }
      }) map { (s.state.initialState, _) }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  val images = reqSnip("images") {
    s =>
      (s.state.initialState.product flatMap { prod =>
        prod.images match {
          case Nil => Success(s.state.initialState, NodeSeq.Empty)
          case images =>
            bind(s.node) {

              case BNode("b:img", a, _) =>

                val p = imagePath(prod.stringId, "normal", prod.images.head)
                val large = imagePath(prod.stringId, "large", prod.images.head)

                BNode("img", a + ("src", p) + ("title", prod.title_?(s.state.lang.name)) + ("data-zoom-image", large)).toElem

              case BNode(e, HasId("thumb", a), _) =>
                NodeSeq.fromSeq(for {
                  p <- prod.images zipWithIndex
                } yield {
                  val normal = imagePath(prod.stringId, "normal", p._1)
                  val large = imagePath(prod.stringId, "large", p._1)
                  val thumb = imagePath(prod.stringId, "thumb", p._1)
                 
                  (BNode(e, a - "id") / <a href="#" data-image={ normal } data-zoom-image={ large }>
                                               <img id={ s"img_${p._2}" } src={ thumb }/>
                                             </a>).toElem
                })

            } map { b => (ProductPageState(s.state.initialState.req, Success(prod), s.state.user), b) }
        }

      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  val detailPrice = reqSnip("detailPrice") {
    s =>
      (for {
        p <- s.state.initialState.product
      } yield {
        (ProductPageState(s.state.initialState.req, Success(p), s.state.user), priceTag(p))
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  val stock = reqSnip("stock") {
    s =>
      (for {
        p <- s.state.initialState.product
      } yield {
        (ProductPageState(s.state.initialState.req, Success(p), s.state.user), p.stock match {
          case None    => Text(Loc.loc0(s.state.lang)("stock.order").text)
          case Some(v) => Text(Loc.loc0(s.state.lang)("in.stock").text)
        })
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  val details = reqSnip("details") {
    s =>
      {
        (for {
          p <- s.state.initialState.product
          desc <- p.description.get(s.state.lang.name)
        } yield {
          (ProductPageState(s.state.initialState.req, Success(p), s.state.user), Text(desc))
        }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
      }
  }

  val specs = reqSnip("specs") {
    s =>
      {
        (for {
          p <- s.state.initialState.product
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
          (ProductPageState(s.state.initialState.req, Success(p), s.state.user), n)
        }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
      }
  }

  val customize = reqSnip("customize") {
    s =>
      {
        (for {
          p <- s.state.initialState.product
        } yield {
          val n = (p.options flatMap {
            o =>
              (bind(s.node) {
                case HasClass("content", a) => <span>{ o._1 }</span><select class="item_option custom_option" name={ o._1 }>{
                  o._2.map { i => <option value={ i }>{ i }</option> }
                }</select>
              }).getOrElse(NodeSeq.Empty)
          }) ++ (p.userText flatMap { o =>
            (bind(s.node) {
              case HasClass("content", a) => <span>{ o }</span><input type="text" name={ s"$o" } class="item_option custom_text"/>
            }).getOrElse(NodeSeq.Empty)
          })
          (ProductPageState(s.state.initialState.req, Success(p), s.state.user), NodeSeq.fromSeq(n.toSeq))
        }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
      }
  }

  val edit = reqSnip("edit") {
    s =>
      {
        (for {
          p <- s.state.initialState.product
        } yield {
          val title = p.title.get(s.state.lang.name).getOrElse("")
          val desc = p.description.get(s.state.lang.name).getOrElse("")
          val discountPrice = p.discountPrice.map(_.toString()).getOrElse("")

          (bind(s.node) {
            case BNode("form", attrs, childs)    => BNode("form", attrs + ("action", ("/product/update/" + p.stringId))) / childs toElem
            case HasId("edit_pid", attrs)            => BNode("input", attrs + ("value", p.stringId)) toElem
            case HasId("edit_title", attrs)          => BNode("input", attrs + ("value", title)) toElem
            case HasId("edit_price", attrs)          => BNode("input", attrs + ("value", p.price.toString())) toElem
            case HasId("edit_discount_price", attrs) => BNode("input", attrs + ("value", discountPrice)) toElem
            case HasId("edit_categories", attrs)     => handleCategories(attrs, s.state.lang, p.categories.toSet) toElem
            case HasId("edit_keywords", attrs)       => BNode("input", attrs + ("value", p.keyWords.mkString(", "))) toElem
            case HasId("edit_stock", attrs)          => BNode("input", attrs + ("value", p.stock.map(_ toString).getOrElse(""))) toElem
            case HasId("edit_unique", attrs) =>
              val a = attrs + ("value", "true")
              BNode("input", if (!p.unique) a else a + ("checked",  p.unique.toString)) toElem
            case HasId("edit_description", attrs)                           => BNode("textarea", attrs) / Text(desc) toElem
            case BNode(_, HasClass("edit_props_sample", attrs), childs) => handleProperties(childs, p)
            case BNode(_, HasClass("edit_user_options", attrs), childs) => handleUserOptions(childs, p)
          }) match {
            case Success(n) => (ProductPageState(s.state.initialState.req, Success(p), s.state.user), n)
            case _          => (ProductPageState(s.state.initialState.req, Success(p), s.state.user), s.node)
          }
        }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
      }
  }

  private def handleCategories(attrs: Attributes, l: Language, categs: Set[String]) = BNode("select", attrs) / {
    ShopApplication.persistence.allCategories match {
      case Success(cats) => NodeSeq.fromSeq((for { c <- cats } yield {
        val opt = BNode("option", Attributes("value", c.stringId)) / Text(c.title_?(l.name))
        if (categs.contains(c.stringId)) {
          (opt addAttr ("selected", "true")).toElem
        } else {
          opt toElem
        }
      }).toSeq)
      case _ => BNode("select", attrs).toElem
    }
  }

  private def handleProperties(childs: NodeSeq, p: ProductDetail) = NodeSeq.fromSeq((p.properties flatMap {
    case (k, v) =>
      bind(BNode("div", Attributes("class", "row")) / childs) {
        case HasName("pkey", attrs) =>
          BNode("input", attrs + ("value", k))
        case HasName("pval", attrs) =>
          BNode("input", attrs + ("value", v))
      } match {
        case Success(n) => n
        case _          => NodeSeq.Empty
      }
  }).toSeq)

  private def handleUserOptions(n: NodeSeq, p: ProductDetail) = {
    bind(n) {
      case BNode(_, HasClass("edit_custom_options_sample", attrs), childs) =>
        NodeSeq.fromSeq(p.options.flatMap {
          case (k, v) =>
            bind(childs) {
              case HasName("customkey", attrs) =>
                BNode("input", attrs + ("value", k))
              case HasName("customval", attrs) =>
                BNode("input", attrs + ("value",  v.mkString(", ")))
            } getOrElse NodeSeq.Empty
        } toList)
      case BNode(_, HasClass("edit_custom_text_sample", attrs), childs) =>
        p.userText.flatMap { t =>
          bind(childs) {
            case HasName("customtext", attrs) =>
              BNode("input", attrs + ("value", t))
          } getOrElse NodeSeq.Empty
        }
    } getOrElse NodeSeq.Empty
  }
}

object ProductPageState {
  def build(req: Request, user: Option[User]): ProductPageState = new ProductPageState(req, Failure[ProductDetail](new RuntimeException("Product not found")), user)
}

case class ProductPageState(req: Request, product: Try[ProductDetail], user: Option[User]) 

