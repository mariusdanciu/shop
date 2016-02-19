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
import net.shop.api.persistence.Persistence
import net.shop.mongodb.MongoDBPersistence
import net.shift.spray.SprayServer
import net.shop.web.pages.AboutUsPage

object StartShop extends App with DefaultLog with IODefaults {

  PropertyConfigurator.configure("config/log4j.properties");

  for { cfg <- Config.load().map { _ append (Map()) } } yield {

    val port = cfg.int("port")
    SprayServer.start(port, ShopApplication(cfg))

    println("Server started on port " + port)
  }
}

object ShopApplication {
  lazy val persistence: Persistence = MongoDBPersistence
  def apply( implicit cfg: Config) = new ShopApplication()
}

class ShopApplication( implicit val cfg: Config ) extends ShiftApplication with ShopServices {

  val orderService = new OrderService
  val productService = new ProductService
  val categoryService = new CategoryService
  val userService = new UserService
  val settingsService = new SettingsService
  
  def servingRule = for {
    r <- withLanguage(Language("ro"))
    c <- staticFiles(Path("web/static")) |
      ajaxLogin |
      ajaxProductsList |
      ajaxCategoriesList |
      ajaxProductDetail |
      ajaxOrdersView |
      ajaxUsersView |
      productsVariantImages |
      categoriesImages |
      page("", Path("web/categories.html"), CategoryPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), new ProductDetailPage()) |
      page("products", Path("web/products.html"), ProductsPage) |
      page("terms", Path("web/terms.html"), TermsPage) |
      page("dataprotection", Path("web/dataprotection.html"), DataProtectionPage) |
      page("returnpolicy", Path("web/returnpolicy.html"), ReturnPolicyPage) |
      page("cookies", Path("web/cookies.html"), CookiesPage) |
      page("aboutus", Path("web/aboutus.html"), AboutUsPage) |
      settingsPage("accountsettings", Path("web/accountsettings.html"), AccountSettingsPage) |
      getCart() |
      orderService.order |
      productService.createProduct |
      productService.deleteProduct |
      productService.updateProduct |
      categoryService.createCategory |
      categoryService.deleteCategory |
      categoryService.updateCategory |
      categoryService.getCategory |
      userService.createUser |
      userService.userInfo |
      userService.deleteThisUser |
      userService.deleteAnyUser |
      userService.forgotPassword |
      settingsService.updateSettings |
      settingsService.updateOrderStatus |
      new OrderService().orderByEmail |
      new OrderService().orderByProduct |
      staticTextFiles("google339a4b5281321c21.html") |
      staticTextFiles("sitemap.xml") |
      notFoundService
    s <- refresh(c)
    t <- tryLogout(r, s)
  } yield t

}



