package net.shop.web.services

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import net.shift.common._
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.page.Html5
import net.shift.template._
import net.shop.backend.Cart
import net.shop.backend.ProductDetail
import net.shop.utils.ShopUtils._
import net.shop.web._
import net.shop.web.ShopApplication._
import net.shop.web.pages._
import scalax.io.Resource
import net.shift.engine.utils.ShiftUtils

object ShopServices {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  implicit val reqSelector = Selectors.bySnippetAttr[SnipState[Request]]
  implicit val cartItemsSelector = Selectors.bySnippetAttr[SnipState[CartState]]

  def page(uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri)
  } yield Html5(r, filePath, snipets)

  def page[T](f: Request => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
  } yield {
    Html5(r, f, filePath, snipets)(Selectors.bySnippetAttr[SnipState[T]])
  }

  def productsImages = for {
    Path("data" :: "products" :: id :: file :: Nil) <- path
  } yield service(resp =>
    resp(new ImageResponse(Resource.fromFile(s"data/products/$id/$file"), "image/jpg")))

  def categoriesImages = for {
    Path("data" :: "categories" :: file :: Nil) <- path
  } yield service(resp =>
    resp(new ImageResponse(Resource.fromFile(s"data/categories/$file"), "image/png")))

  def getCart() = for {
    r <- req
    Path("getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        val json = java.net.URLDecoder.decode(c.value, "UTF-8")
        for {
          input <- ShiftUtils.fromFile(Path("web/templates/cartitem.html"))
          template <- XmlUtils.load(input)
        } yield {
          val list = for {
            item <- parse(json).extract[Cart].items
            prod <- ShopApplication.productsService.productById(item.id).toOption
          } yield {
            new Html5(CartState(item.count, prod), r.language, CartItemNode).resolve(template).toString
          }
          resp(JsonResponse(write(list)))
        }
      }
      case _ => resp(JsonResponse("[]"))
    })
}


