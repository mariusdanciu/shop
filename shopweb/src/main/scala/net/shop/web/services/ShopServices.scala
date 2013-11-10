package net.shop.web.services

import net.shift.engine.ShiftApplication.service
import net.shift.engine.ShiftApplication.serviceWithRequest
import net.shift.engine.http.AsyncResponse
import net.shift.engine.http.HttpPredicates.path
import net.shift.engine.http.ImageResponse
import net.shift.engine.http.JsonResponse
import net.shift.engine.http.Request
import net.shift.engine.http.TextResponse
import net.shift.engine.http.Html5Response
import net.shift.engine.page.Html5
import net.shift.template._
import net.shift.common._
import org.json4s._
import org.json4s.native.JsonMethods._
import native.Serialization.write
import scalax.io.Resource
import net.shop.backend.Cart
import net.shop.web._
import pages._
import ShopApplication._
import net.shop.utils.ShopUtils._
import net.shop.backend.ProductDetail

object ShopServices {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  def page(uri: String, filePath: String, snipets: DynamicContent[Request]) = for {
    _ <- path(uri)
  } yield Html5(filePath, snipets)

  def page[T](f: Request => T, uri: String, filePath: String, snipets: DynamicContent[T]) = for {
    _ <- path(uri)
  } yield Html5(f, filePath, snipets)

  def productsImages = for {
    "data" :: "products" :: id :: file :: Nil <- path
  } yield service(resp => resp(new ImageResponse(Resource.fromFile("data/products/" + id + "/" + file), "image/jpg")))

  def getCart() = for {
    "getcart" :: Nil <- path
  } yield serviceWithRequest(r => resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        val json = java.net.URLDecoder.decode(c.value, "UTF-8")
        val template = XmlUtils.load(r.resource("web/templates/cartitem.html"))
            
        val list = for {
          item <- parse(json).extract[Cart].items
          prod <- ShopApplication.productsService.byId(item.id).toOption
        } yield {
          new Html5(CartState(item.count, prod), Selectors.bySnippetAttr[SnipState[CartState]])(CartItemNode).resolve(template).toString
        }
        resp(JsonResponse(write(list)))
      }
      case _ => resp(JsonResponse("[]"))
    })
}


