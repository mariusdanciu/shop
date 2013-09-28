package net.shop
package web

import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication._
import net.shift.engine.http._
import HttpPredicates._
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils.cssFromFolder
import net.shift.engine.utils.ShiftUtils.imagesFromFolder
import net.shift.engine.utils.ShiftUtils.jsFromFolder
import net.shift.netty.NettyServer
import net.shift.template.DynamicContent
import net.shop.backend.ProductsService
import net.shop.backend.impl.FSProductsService
import scalax.io.Resource
import net.shop.web.pages._
import scala.xml.Elem
import net.shift.common._

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
      imagesFromFolder("web/images") |
      productsImages |
      page(ProductPageState.build _, "/product", "web/product.html", ProductDetailPage) |
      page("/", "web/index.html", IndexPage) |
      service(notFoundService)

}

object ShopUtils {

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

}

