package net.shop.web.services

import scala.Option.option2Iterable
import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.AsyncResponse
import net.shift.engine.http.ImageResponse
import net.shift.engine.http.JsonResponse
import net.shift.engine.http.Request
import net.shift.engine.http.TextResponse
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils
import net.shift.template.DynamicContent
import net.shift.template.Selectors
import net.shift.template.SnipState
import net.shop.web.ShopApplication
import net.shop.web.pages.CartItemNode
import net.shop.web.pages.CartState
import scalax.io.Resource
import net.shop.model.Cart
import net.shop.web.form.OrderForm
import net.shift.html.Success
import net.shift.html.Failure
import net.shift.js
import net.shift.js.JsDsl
import net.shift.js.JsStatement
import net.shift.engine.http.JsResponse
import net.shift.loc.Loc
import net.shift.common.PathUtils

trait ShopServices extends PathUtils with ShiftUtils with Selectors {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  implicit val reqSelector = bySnippetAttr[SnipState[Request]]
  implicit val cartItemsSelector = bySnippetAttr[SnipState[CartState]]

  def page(uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri)
  } yield Html5(r, filePath, snipets)

  def page[T](f: Request => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
  } yield {
    Html5(r, f, filePath, snipets)(bySnippetAttr[SnipState[T]])
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
    Path("getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        for {
          input <- fromPath(Path("web/templates/cartitem.html"))
          template <- load(input)
        } yield {
          val list = for {
            item <- readCart(c.value).items
            prod <- ShopApplication.productsService.productById(item.id).toOption
          } yield {
            new Html5(CartState(item.count, prod), r.language, CartItemNode).resolve(template).toString
          }
          resp(JsonResponse(write(list)))
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


