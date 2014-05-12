package net.shop
package web

import net.shift.common.State._
import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication._
import net.shift.engine.utils.ShiftUtils
import net.shift.netty.NettyServer
import net.shift.template.DynamicContent
import net.shop.backend.ProductsService
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
import net.shop.backend.impl.CachingBackend
import scala.concurrent.ExecutionContext.Implicits.global
import net.shop.web.pages.ProductsQuery
import net.shift.common.DefaultLog
import net.shop.web.pages.Cart
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.TermsPage

object StartShop extends App with DefaultLog {

  PropertyConfigurator.configure("config/log4j.properties");

  info("Starting iDid application ...");

  Config load ()

  NettyServer.start(Config.int("port"), ShopApplication)
}

object ShopApplication extends ShiftApplication with ShopServices {

  def productsService(lang: Language): ProductsService = new CachingBackend(FSProductsService(lang))

  def ajaxServices = for {
    r <- ajax
    p <- page("products", Path("web/templates/productslist.html"), ProductsPage)
  } yield p

  def servingRule = for {
    _ <- withLanguage(Language("ro"))
    c <- ajaxServices |
      staticFiles(Path("web/static")) |
      productsImages |
      productsVariantImages |
      categoriesImages |
      page("/", Path("web/categories.html"), CategoryPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), ProductDetailPage) |
      page("products", Path("web/products.html"), ProductsPage) |
      page("/terms", Path("web/terms.html"), TermsPage) |
      getCart() |
      order.get |
      service(notFoundService)
  } yield c

}



