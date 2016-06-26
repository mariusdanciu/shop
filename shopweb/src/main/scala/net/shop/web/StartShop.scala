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
import net.shop.web.services.mobile.MobileServices

object StartShop extends App with DefaultLog with IODefaults {

  PropertyConfigurator.configure("config/log4j.properties");

  for { cfg <- Config.load() } yield {

    val port = cfg.int("port")
    val dbPass = args.apply(0)

    implicit val c = cfg append Map("db.pwd" -> dbPass)

    SprayServer.start(port, ShopApplication())

    println("Server started on port " + port)
  }

}

object ShopApplication {
  def apply()(implicit cfg: Config) = new ShopApplication(cfg)
}

class ShopApplication(c: Config) extends ShiftApplication with ShopServices { self =>

  implicit val cfg = c
  implicit val store: Persistence = new MongoDBPersistence

  val mobile = Mobile(cfg, store)

  val orderService = new OrderService {
    val cfg = self.cfg
    val store = self.store
  }
  val productService = new ProductService {
    val cfg = self.cfg
    val store = self.store
  }
  val categoryService = new CategoryService {
    val cfg = self.cfg
    val store = self.store
  }
  val userService = new UserService {
    val cfg = self.cfg
    val store = self.store
  }
  val settingsService = new SettingsService {
    val cfg = self.cfg
    val store = self.store
  }

  val catPage = new CategoryPage {
    val cfg = self.cfg
    val store = self.store
  }

  val prodDetailPage = new ProductDetailPage {
    val cfg = self.cfg
    val store = self.store
  }

  val productsPage = new ProductsPage {
    val cfg = self.cfg
    val store = self.store
  }

  val accPage = new AccountSettingsPage {
    val cfg = self.cfg
    val store = self.store
  }

  val mobileServices = new MobileServices {
    val cfg = self.cfg
    val store = self.store
  }

  def servingRule = for {
    r <- withLanguage(Language("ro"))
    c <- toMobileIfNeeded |
      staticFiles(Path("web/static")) |
      mobile.mobilePages |
      mobileServices.cartView |
      ajaxLogin |
      ajaxProductsList |
      ajaxCategoriesList |
      ajaxProductDetail |
      ajaxOrdersView |
      ajaxUsersView |
      productsVariantImages |
      categoriesImages |
      categoriesMobileImages |
      page("", Path("web/categories.html"), catPage) |
      page(ProductPageState.build _, "product", Path("web/product.html"), prodDetailPage) |
      page("products", Path("web/products.html"), productsPage) |
      page("terms", Path("web/terms.html"), TermsPage) |
      page("dataprotection", Path("web/dataprotection.html"), DataProtectionPage) |
      page("returnpolicy", Path("web/returnpolicy.html"), ReturnPolicyPage) |
      page("cookies", Path("web/cookies.html"), CookiesPage) |
      page("aboutus", Path("web/aboutus.html"), AboutUsPage) |
      settingsPage("accountsettings", Path("web/accountsettings.html"), accPage) |
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
      orderService.orderByEmail |
      orderService.orderByProduct |
      staticTextFiles("google339a4b5281321c21.html") |
      staticTextFiles("sitemap.xml") |
      notFoundService
    s <- refresh(c)
    t <- tryLogout(r, s)
  } yield t

}



