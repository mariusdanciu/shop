package net.shop
package web.services

import scala.Option.option2Iterable
import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.http.ImageResponse
import net.shift.engine.http.Request
import net.shift.engine.http.TextResponse
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils
import net.shift.template.DynamicContent
import net.shift.template.Selectors
import net.shift.template.SnipState
import net.shop.model.Cart
import net.shop._
import web.pages.CartState
import web.pages.CartItemNode
import scala.util.Success
import net.shift.engine.http.JsonResponse
import scala.util.Failure
import net.shift.common.Log
import net.shop.web.ShopApplication
import net.shop.web.pages.ProductsPage
import scala.util.Try
import net.shop.model.ProductDetail
import net.shift.engine.http.JsResponse
import net.shop.web.pages.ProductsQuery
import net.shift.loc.Language
import utils.ShopUtils._

trait ShopServices extends PathUtils with ShiftUtils with Selectors with TraversingSpec with Log {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  implicit val reqSelector = bySnippetAttr[SnipState[Request]]
  implicit val cartItemsSelector = bySnippetAttr[SnipState[CartState]]

  def page[T](uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri)
  } yield {
    Html5.pageFromFile(r, r.language, filePath, snipets)
  }

  def page[T](f: Request => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
  } yield {
    Html5.pageFromFile(f(r), r.language, filePath, snipets)(bySnippetAttr[SnipState[T]])
  }

  def productsImages = for {
    Path("data" :: "products" :: id :: file :: Nil) <- path
    input <- fileOf(Path(s"data/products/$id/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/jpg")))

  def productsVariantImages = for {
    Path("data" :: "products" :: id :: variant :: file :: Nil) <- path
    input <- fileOf(Path(s"data/products/$id/$variant/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/jpg")))

  def categoriesImages = for {
    Path("data" :: "categories" :: file :: Nil) <- path
    input <- fileOf(Path(s"data/categories/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/png")))

  def getCart() = for {
    r <- req
    lang <- language
    Path("getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]

        listTraverse.sequence(for {
          item <- readCart(c.value).items
          prod <- ShopApplication.productsService(lang).productById(item.id).toOption
        } yield {
          Html5.runPageFromFile(CartState(item.count, prod), r.language, Path("web/templates/cartitem.html"), CartItemNode).map(_._2 toString)
        }) match {
          case Success(list) =>
            resp(JsonResponse(write(list)))
          case Failure(t) => error("Cannot process cart", t)
        }

      }
      case _ => resp(JsonResponse("[]"))
    })

  def order = OrderService

  private def readCart(json: String): Cart = {
    implicit val formats = DefaultFormats
    parse(java.net.URLDecoder.decode(json, "UTF-8")).extract[Cart]
  }

}


