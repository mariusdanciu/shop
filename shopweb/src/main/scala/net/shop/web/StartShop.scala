package net.shop
package web

import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication.rule
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.AsyncResponse
import net.shift.engine.http.HttpPredicates.path
import net.shift.engine.http.ImageResponse
import net.shift.engine.http.Request
import net.shift.engine.http.TextResponse
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils.cssFromFolder
import net.shift.engine.utils.ShiftUtils.jpgFromFolder
import net.shift.engine.utils.ShiftUtils.jsFromFolder
import net.shift.netty.NettyServer
import net.shift.template.DynamicContent
import net.shop.backend.ProductsService
import net.shop.backend.impl.FSProductsService
import net.shop.web.pages.ProductDetailPage
import pages.IndexPage
import scalax.io.Resource

object StartShop extends App {
  import ShopUtils._

  println("Starting shop application ...");

  NettyServer.start(8080, ShopApplication)
}

object ShopApplication extends ShiftApplication {
  import ShopUtils._

  def productsService: ProductsService = new FSProductsService

  def servingRule =
    cssFromFolder("web/styles") |
      jsFromFolder("web/scripts") |
      jpgFromFolder("web/images") |
      productsImages |
      page("/product", "web/product.html", ProductDetailPage) |
      page("/", "web/index.html", IndexPage) |
      service(notFoundService)

}

object ShopUtils {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  def page(uri: String, filePath: String, snipets: DynamicContent[Request]) = for {
    _ <- path(uri)
  } yield Html5(filePath, IndexPage)

  def productsImages = for {
    "data" :: "products" :: id :: file :: Nil <- path
  } yield service(resp => resp(new ImageResponse(Resource.fromFile("data/products/" + id + "/" + file), "image/jpg")))

}

