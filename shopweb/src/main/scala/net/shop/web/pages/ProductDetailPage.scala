package net.shop
package web.pages

import net.shift.common.XmlImplicits._
import net.shift.common.{Path, ShiftFailure, Xml, XmlAttr}
import net.shift.loc.{Language, Loc}
import net.shift.server.http.Request
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template._
import net.shop.model.{ProductDetail, ShopError}
import net.shop.utils.{LargePic, NormalPic, ShopUtils, ThumbPic}
import net.shop.utils.ShopUtils._

import scala.util.{Failure, Success, Try}
import scala.xml._

trait ProductDetailPage extends PageCommon[ProductPageState] {

  override def snippets = List(checkProd, images, detailPrice, stock, specs, edit) ++ super.snippets
  override def inlines = List(saveUrl, title, pageUrl, prodImageUrl, desc, prodId) ++ super.inlines

  val prodId = inline[ProductPageState]("prodId") {
    s =>
      product(s) map {
        p => (s.state.initialState, s"${p.stringId}")
      }
  }
  val pageUrl = inline[ProductPageState]("pageUrl") {
    s =>
      product(s) map {
        p => (s.state.initialState, "http://" + cfg.string("host", "idid.ro") + s"/product/${ShopUtils.nameToPath(p)}")
      }
  }
  val prodImageUrl = inline[ProductPageState]("prodImageUrl") {
    s =>
      product(s) map {
        p => (s.state.initialState, "http://" + cfg.string("host", "idid.ro") + imagePath(NormalPic, p))
      }
  }
  val checkProd = reqSnip("checkProd") {
    s =>
      product(s) match {
        case Success(p) =>
          Success((ProductPageState(s.state.initialState.req, Success(p)), s.node))
        case Failure(f) => Success((ProductPageState(s.state.initialState.req, Failure(f)), NodeSeq.Empty))
      }
  }
  val images = reqSnip("images") {
    s =>
      (product(s) flatMap { prod =>
        prodImageFiles(prod.stringId) match {
          case Nil => Success(s.state.initialState, NodeSeq.Empty)
          case images =>
            bind(s.node) {

              case Xml("b:img", a, _) =>

                val p = imagePath(NormalPic, prod.stringId)
                val large = imagePath(LargePic, prod.stringId)

                Xml("img", a + ("src", p) + ("alt", prod.title_?(s.state.lang.name) + ShopUtils.OBJECT_SUFFIX) + ("data-zoom-image", large))

              case Xml(e, HasId("thumb", a), _) =>
                NodeSeq.fromSeq(for {
                  p <- images zipWithIndex
                } yield {
                  val normal = imagePath(NormalPic, prod.stringId, p._1)
                  val large = imagePath(LargePic, prod.stringId, p._1)
                  val thumb = imagePath(ThumbPic, prod.stringId, p._1)

                  (Xml(e, a - "id") / <a href="#" data-image={normal} data-zoom-image={large}>
                    <img id={s"img_${p._2}"} src={thumb} alt={prod.title_?(s.state.lang.name) + ShopUtils.OBJECT_SUFFIX}/>
                  </a>)
                })

            } map { b => (ProductPageState(s.state.initialState.req, Success(prod)), b) }
        }

      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }
  val detailPrice = reqSnip("detailPrice") {
    s =>
      (for {
        p <- product(s)
      } yield {
        (ProductPageState(s.state.initialState.req, Success(p)), priceTag(p))
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }
  val stock = reqSnip("stock") {
    s =>
      (for {
        p <- product(s)
      } yield {
        (ProductPageState(s.state.initialState.req, Success(p)), p.stock match {
          case None => Text(Loc.loc0(s.state.lang)("stock.order").text)
          case Some(v) => Text(Loc.loc0(s.state.lang)("in.stock").text)
        })
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }
  val specs = reqSnip("specs") {
    s => {
      (for {
        p <- product(s)
      } yield {
        val n = (NodeSeq.Empty /: p.properties) {
          case (acc, (k, v)) =>
            (bind(s.node) {
              case HasClass("prop_name", a) => Text(k)
              case HasClass("prop_value", a) => Text(v)
            }) match {
              case Success(n) => acc ++ n
              case _ => NodeSeq.Empty
            }
        }
        (ProductPageState(s.state.initialState.req, Success(p)), n)
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
    }
  }
  val edit = reqSnip("edit") {
    s => {
      (for {
        p <- product(s)
      } yield {
        val title = p.title.get(s.state.lang.name).getOrElse("")
        val desc = p.description.get(s.state.lang.name).getOrElse("")
        val discountPrice = p.discountPrice.map(_.toString()).getOrElse("")

        bind(s.node) {
          case Xml("form", attrs, childs) => Xml("form", attrs + ("action", ("/product/update/" + p.stringId))) / childs
          case HasId("edit_pid", attrs) => Xml("input", attrs + ("value", p.stringId))
          case HasId("edit_title", attrs) => Xml("input", attrs + ("value", title))
          case HasId("edit_price", attrs) => Xml("input", attrs + ("value", p.price.toString()))
          case HasId("edit_discount_price", attrs) => Xml("input", attrs + ("value", discountPrice))
          case HasId("edit_categories", attrs) => handleCategories(attrs, s.state.lang, p.categories.toSet)
          case HasId("edit_keywords", attrs) => Xml("input", attrs + ("value", p.keyWords.mkString(", ")))
          case HasId("edit_stock", attrs) => Xml("input", attrs + ("value", p.stock.map(_ toString).getOrElse("")))
          case HasId("edit_pos", attrs) => Xml("input", attrs + ("value", p.position.map(_ toString).getOrElse("")))
          case HasId("edit_pres_pos", attrs) => Xml("input", attrs + ("value", p.presentationPosition.map(_ toString).getOrElse("")))
          case HasId("edit_unique", attrs) =>
            val a = attrs + ("value", "true")
            Xml("input", if (!p.unique) a else a + ("checked", p.unique.toString))
          case HasId("edit_description", attrs) => Xml("textarea", attrs) / Text(desc)
          case n@HasId("edit_prop_fields", attrs) =>
            val res = for {(k, v) <- p.properties} yield {
              bind(n.child) {
                case e@HasName("pkey", attrs) => Xml(e.label, XmlAttr(attrs.attrs + ("value" -> k)))
                case e@HasName("pval", attrs) => Xml(e.label, XmlAttr(attrs.attrs + ("value" -> v)))
              }.getOrElse(NodeSeq.Empty).flatten
            }

            NodeSeq.fromSeq(res.flatten.toSeq)

        } match {
          case Success(n) => (ProductPageState(s.state.initialState.req, Success(p)), n)
          case _ => (ProductPageState(s.state.initialState.req, Success(p)), s.node)
        }
      }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
    }
  }


  def saveUrl = inline[ProductPageState]("saveurl") {
    s =>
      product(s) map {
        p => (s.state.initialState, s"/saveproduct/${p.stringId}")
      }
  }

  def title = inline[ProductPageState]("title") {
    s =>
      product(s) map {
        p => (s.state.initialState, p.title_?(s.state.lang.name))
      }
  }

  def desc = inline[ProductPageState]("desc") {
    s =>
      product(s) map {
        p => (s.state.initialState, p.description.get(s.state.lang.name) getOrElse "")
      }
  }

  def product(s: SnipState[ProductPageState]): Try[ProductDetail] = {
    s.state.initialState.product match {
      case Failure(t) =>
        Path(s.state.initialState.req.uri.path) match {
          case Path(_, _ :: _ :: name :: _) =>
            store.productByName(extractName(name)) match {
              case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
              case Failure(t) => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
              case s => s
            }
          case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
        }
      case s => s
    }
  }


  private def ignoreNode(s: SnipState[ProductPageState], f: ProductDetail => Boolean) = {
    val node = for {p <- product(s)} yield {
      if (f(p)) {
        NodeSeq.Empty
      } else {
        s.node
      }
    }
    (node map { l => (s.state.initialState, l) }).recover { case _ => (s.state.initialState, NodeSeq.Empty) }
  }

  private def handleCategories(attrs: XmlAttr, l: Language, categs: Set[String]) = Xml("select", attrs) / {
    store.allCategories match {
      case Success(cats) => NodeSeq.fromSeq((for {c <- cats} yield {
        val opt = Xml("option", XmlAttr("value", c.stringId)) / Text(c.title_?(l.name))
        if (categs.contains(c.stringId)) {
          (opt addAttr("selected", "true"))
        } else {
          opt
        }
      }).toSeq)
      case _ => Xml("select", attrs)
    }
  }

}

object ProductPageState {
  def build(req: Request): ProductPageState = new ProductPageState(req, Failure[ProductDetail](new RuntimeException("Product not found")))
}

case class ProductPageState(req: Request, product: Try[ProductDetail]) 

