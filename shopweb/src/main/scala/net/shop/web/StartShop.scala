package net.shop
package web

import net.shift.common._
import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication._
import net.shift.engine.http._
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils.cssFromFolder
import net.shift.engine.utils.ShiftUtils.imagesFromFolder
import net.shift.engine.utils.ShiftUtils.jsFromFolder
import net.shift.netty.NettyServer
import net.shift.template.DynamicContent
import net.shop.backend.ProductsService
import net.shop.backend.impl.FSProductsService
import net.shop.web.pages._
import scalax.io.Resource
import scala.util.Success
import net.shop.utils.ShopUtils._
import net.shop.web.services.ShopServices

object StartShop extends App {

  println("Starting shop application ...");

  NettyServer.start(8080, ShopApplication)
}

object ShopApplication extends ShiftApplication {
  import ShopServices._

  def productsService: ProductsService = new FSProductsService

  def servingRule =
    cssFromFolder("web/styles") |
      jsFromFolder("web/scripts") |
      imagesFromFolder("web/images") |
      productsImages |
      page(ProductPageState.build _, "/product", "web/product.html", ProductDetailPage) |
      page("/", "web/index.html", IndexPage) |
      getCart() |
      service(notFoundService)

}



