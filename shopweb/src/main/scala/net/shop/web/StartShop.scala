package net.shop
package web

import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.log4j.PropertyConfigurator
import net.shift.common.Config
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.engine.ShiftApplication
import net.shift.engine.ShiftApplication.rule
import net.shift.engine.ShiftApplication.service
import net.shift.loc.Language
import net.shift.netty.NettyServer
import net.shop.api.persistence.Persistence
import net.shop.mongodb.MongoDBPersistence
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.ProductDetailPage
import net.shop.web.pages.ProductPageState
import net.shop.web.pages.ProductsPage
import net.shop.web.pages.UserState
import net.shop.web.pages.TermsPage
import net.shop.web.services.ShopServices
import net.shop.web.pages.UserState

object StartShop extends App with DefaultLog {

  PropertyConfigurator.configure("config/log4j.properties");

  info("Starting iDid application ...");

  Config load ()

  NettyServer.start(Config.int("port"), ShopApplication)

  println("Server started.")
}

object ShopApplication extends ShiftApplication with ShopServices {

  lazy val persistence: Persistence = MongoDBPersistence

  def logReq = for {
    r <- req
  } yield {
    log.debug("Request: " + r.uri)
    r
  }

  def ajaxProductsList = for {
    r <- ajax
    p <- page(UserState.build _, "products", Path("web/templates/productslist.html"), ProductsPage)
  } yield p

  def ajaxProductDetail = for {
    r <- ajax
    p <- page(ProductPageState.build _, "productquickview", Path("web/templates/productquickview.html"), ProductDetailPage)
  } yield p

  def servingRule = for {
    _ <- logReq
    _ <- withLanguage(Language("ro"))
    c <- ajaxProductsList |
      ajaxProductDetail |
      staticFiles(Path("web/static")) |
      productsVariantImages |
      categoriesImages |
      page(UserState.build _, "/", Path("web/categories.html"), CategoryPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), ProductDetailPage) |
      page(UserState.build _, "products", Path("web/products.html"), ProductsPage) |
      page("terms", Path("web/terms.html"), TermsPage) |
      authPage(UserState.build _, "admin", Path("web/categories.html"), CategoryPage) |
      getCart() |
      orderService.order |
      createProduct |
      deleteProduct |
      updateProduct |
      service(notFoundService)
  } yield c

}



