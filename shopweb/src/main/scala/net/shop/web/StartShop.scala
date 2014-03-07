package net.shop
package web

import net.shift.common.State._
import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication._
import net.shift.engine.utils.ShiftUtils
import net.shift.netty.NettyServer
import net.shift.template.DynamicContent
import net.shop.backend.ProductsService
import net.shop.utils.ShopUtils._
import net.shop.web.services.ShopServices
import net.shop.backend.impl.FSProductsService
import net.shift.engine.http.Request
import net.shift.loc.Language
import net.shift.common.Path
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.ProductPageState
import net.shop.web.pages.ProductDetailPage
import net.shop.web.pages.ProductsPage
import net.shift.common.Log
import net.shift.common.Config
import org.apache.log4j.PropertyConfigurator

object StartShop extends App with Log {

  PropertyConfigurator.configure("config/log4j.properties");
  
  info("Starting iDid application ...");

  Config load ()

  NettyServer.start(Config.int("port"), ShopApplication)
}

object ShopApplication extends ShiftApplication with ShopServices {

  def productsService: ProductsService = new FSProductsService

  def fixLang = initf[Request](_.withLanguage(Language("ro")))

  def servingRule = for {
    _ <- fixLang
    c <- cssFromFolder(Path("web/styles")) |
      jsFromFolder(Path("web/scripts")) |
      imagesFromFolder(Path("web/images")) |
      productsImages |
      productsVariantImages |
      categoriesImages |
      page("/", Path("web/categories.html"), CategoryPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), ProductDetailPage) |
      page("products", Path("web/products.html"), ProductsPage) |
      getCart() |
      order.get |
      service(notFoundService)
  } yield c

}



