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
import net.shop.mongodb.MongoDBPersistence
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.LoginPage
import net.shop.web.pages.ProductDetailPage
import net.shop.web.pages.ProductPageState
import net.shop.web.pages.ProductsPage
import net.shop.web.pages.TermsPage
import net.shop.web.services.ShopServices
import net.shop.api.persistence.Persistence
import net.shop.web.pages.AccountSettingsPage
import net.shop.web.services.UserService
import net.shop.web.services.SettingsService
import net.shop.web.services.CategoryService
import net.shop.web.services.ProductService
import net.shop.web.services.OrderService
import net.shop.web.services.ShopServices
import net.shop.web.pages.ReturnPolicyPage
import net.shop.web.pages.DataProtectionPage
import net.shift.io.IODefaults
import net.shop.web.pages.CookiesPage

object StartShop extends App with DefaultLog with IODefaults {

  PropertyConfigurator.configure("config/log4j.properties");

  Config load ()

  val port = Config.int("port")
  NettyServer.start(port, ShopApplication)

  println("Server started on port " + port)
}

object ShopApplication extends ShiftApplication with ShopServices {

  lazy val persistence: Persistence = MongoDBPersistence

  def servingRule = for {
    _ <- traceStats
    r <- withLanguage(Language("ro"))
    c <- staticFiles(Path("web/static")) |
      ajaxLogin |
      ajaxProductsList |
      ajaxCategoriesList |
      ajaxProductDetail |
      ajaxOrdersView |
      productsVariantImages |
      categoriesImages |
      page("", Path("web/categories.html"), CategoryPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), ProductDetailPage) |
      page("products", Path("web/products.html"), ProductsPage) |
      page("terms", Path("web/terms.html"), TermsPage) |
      page("dataprotection", Path("web/dataprotection.html"), DataProtectionPage) |
      page("returnpolicy", Path("web/returnpolicy.html"), ReturnPolicyPage) |
      page("cookies", Path("web/cookies.html"), CookiesPage) |
      settingsPage("accountsettings", Path("web/accountsettings.html"), AccountSettingsPage) |
      getCart() |
      OrderService.order |
      ProductService.createProduct |
      ProductService.deleteProduct |
      ProductService.updateProduct |
      CategoryService.createCategory |
      CategoryService.deleteCategory |
      CategoryService.updateCategory |
      CategoryService.getCategory |
      UserService.createUser |
      UserService.userInfo |
      UserService.forgotPassword |
      SettingsService.updateSettings |
      SettingsService.updateOrderStatus |
      OrderService.orderByEmail |
      OrderService.orderByProduct |
      notFoundService
    s <- refresh(c)
    t <- tryLogout(r, s)
  } yield t

}



