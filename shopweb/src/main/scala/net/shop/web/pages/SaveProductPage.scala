package net.shop.web.pages

import net.shift.common.XmlImplicits._
import net.shift.common.{Path, ShiftFailure, Xml, XmlAttr}
import net.shift.loc.Loc
import net.shift.template.Binds.bind
import net.shift.template.SnipState
import net.shift.template.Snippet._
import net.shop.api.{ProductDetail, ShopError}
import net.shop.utils.ShopUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, Text}

trait SaveProductPage extends PageCommon[ProductPageState] {
  self =>

  override def inlines = List(prod, path, fieldPrefix) ++ super.inlines

  override def snippets = List(catList, delImgList) ++ super.snippets


  val prod = inline[ProductPageState]("prod") {
    s =>
      product(s) match {
        case Success(p) =>
          val out = s.params.head match {
            case "title" => p.title_?(s.state.lang.name)
            case "price" => ShopUtils.price(p.price)
            case "discount" => p.discountPrice map {
              ShopUtils.price
            } getOrElse ""
            case "keywords" => p.keyWords mkString ","
            case "stock" => p.stock getOrElse "" toString
            case "pos" => p.position getOrElse "" toString
            case "unique" => p.unique.toString()
            case "specs" => p.properties map { case (k, v) => s"$k=$v" } mkString "\n"
            case "desc" => p.description_?(s.state.lang.name)
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
          val res = NodeSeq.fromSeq((for {c <- cats} yield {
            if (prodCats.contains(c.stringId)) {
              <option value={c.stringId} selected="true">
                {Text(c.title_?(s.state.initialState.req.language.name))}
              </option>
            } else
              Xml("option", XmlAttr("value", c.stringId)) / Text(c.title_?(s.state.initialState.req.language.name))
          }).toSeq)
          Success((s.state.initialState, res))
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }
  }

  val delImgList = reqSnip("delimglist") {
    s =>
      val xml = s.state.initialState.product map { p =>

        (ShopUtils.productImages("thumb", p) zip p.images).map {
          case (img, name) =>
            val red: NodeSeq = bind(s.node) {
              case Xml("img", a, _) => <img src={img}/>
              case Xml("input", a, _) => <input value={name}/> % a
            } getOrElse NodeSeq.Empty
            red
        }

      } getOrElse NodeSeq.Empty

      Success((s.state.initialState, xml flatten))

  }


  val path = inline[ProductPageState]("path") {
    s =>
      product(s) match {
        case Success(p) => Success(s.state.initialState -> s"/product/update/${p.stringId}")
        case _ => Success(s.state.initialState -> "/product/create")
      }
  }
  val fieldPrefix = inline[ProductPageState]("field_prefix") {
    s =>
      product(s) match {
        case Success(p) => Success(s.state.initialState -> "edit")
        case _ => Success(s.state.initialState -> "create")
      }
  }

  def product(s: SnipState[ProductPageState]): Try[ProductDetail] = {
    s.state.initialState.product match {
      case Failure(t) =>
        Path(s.state.initialState.req.uri.path) match {
          case Path(_, _ :: _ :: id :: _) =>
            store.productById(id) match {
              case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
              case Failure(t) => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
              case s => s
            }
          case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
        }
      case s => s
    }
  }


}