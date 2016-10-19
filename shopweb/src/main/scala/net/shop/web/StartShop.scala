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
import net.shop.web.pages.AboutUsPage
import net.shift.engine.http.HttpPredicates._
import net.shop.web.pages.CookiesPage
import net.shop.web.pages.DataProtectionPage
import net.shop.web.pages.TermsPage
import net.shift.io.LocalFileSystem
import net.shop.web.pages.ReturnPolicyPage
import net.shop.web.pages.AboutUsPage
import net.shop.web.pages.CartPage
import net.shop.web.pages.CartInfo
import net.shift.server.Server
import net.shop.web.pages.SaveProductPage
import net.shift.loc.Loc
import net.shift.security.Permission
import net.shift.server.http.Request
import net.shift.security.User
import net.shop.web.pages.NewUserPage
import net.shift.server.ServerSpecs
import net.shift.server.http.HttpProtocol
import net.shift.server.http.HttpProtocolBuilder

object StartShop extends App with DefaultLog {

  implicit val fs = LocalFileSystem
  PropertyConfigurator.configure("config/log4j.properties");

  for { cfg <- Config.load(profile) } yield {

    val port = cfg.int("server.port")
    val dbPass = args.apply(0)

    implicit val c = cfg append Map("db.pwd" -> dbPass)

    log.info("Configs " + c.configs)

    Server(ServerSpecs.fromConfig(c)).start(HttpProtocolBuilder(ShopApplication().shiftService))

    println("Server started on port " + port)
  }

  def profile = {
    var prof = System.getProperty("config.profile")
    if (prof == null) {
      ""
    } else {
      s"-$prof"
    }
  }

}

object ShopApplication {
  def apply()(implicit cfg: Config) = new ShopApplication(cfg)
}

class ShopApplication(c: Config) extends ShiftApplication with ShopServices { self =>

  implicit val cfg = c
  implicit val store: Persistence = new MongoDBPersistence

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

  val cartPage = new CartPage {
    val cfg = self.cfg
    val store = self.store
  }

  val saveProductPage = new SaveProductPage {
    val cfg = self.cfg
    val store = self.store
  }

  val newUserPage = new NewUserPage {
    val cfg = self.cfg
    val store = self.store
  }

  def servingRule = for {
    r <- withLanguage(Language("ro"))
    u <- user
    c <- staticFiles(Path("web/static")) |
      ajaxLogin |
      productsVariantImages |
      categoriesImages |
      logout |
      page("/", Path("web/categories.html"), catPage) |
      page("/products", Path("web/products.html"), productsPage) |
      page("/terms", Path("web/terms.html"), TermsPage) |
      page("/dataprotection", Path("web/dataprotection.html"), DataProtectionPage) |
      page("/returnpolicy", Path("web/returnpolicy.html"), ReturnPolicyPage) |
      page("/cookies", Path("web/cookies.html"), CookiesPage) |
      page("/aboutus", Path("web/aboutus.html"), AboutUsPage) |
      page("/cart", Path("web/cart.html"), cartPage, CartInfo(r, Nil)) |
      page("/newuser", Path("web/newuser.html"), newUserPage) |
      settingsPage("/accountsettings", Path("web/accountsettings.html"), accPage) |
      product(r, u) |
      saveProduct(r, u) |
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
      userService.forgotPassword |
      settingsService.updateSettings |
      settingsService.updateOrderStatus |
      orderService.orderByEmail |
      orderService.orderByProduct |
      staticFile(Path("/google339a4b5281321c21.html")) |
      staticFile(Path("/sitemap.xml")) |
      notFoundService
    s <- refresh(c)
  } yield s

  def saveProduct(req: Request, u: Option[User]) = pageWithRules(Path("web/saveproduct.html"), saveProductPage,
    for {
      _ <- get
      Path(_, _ :: "saveproduct" :: _) <- path
      _ <- userRequired(Loc.loc0(req.language)("login.fail").text)
      r <- permissions("Unauthorized", Permission("write"))
    } yield r, ProductPageState.build(req, u))

  def product(req: Request, u: Option[User]) = pageWithRules(Path("web/product.html"), prodDetailPage,
    for {
      _ <- get
      Path(_, _ :: "product" :: _) <- path
    } yield (), ProductPageState.build(req, u))

}



