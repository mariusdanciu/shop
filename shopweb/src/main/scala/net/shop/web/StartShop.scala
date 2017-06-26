package net.shop
package web

import net.shift.common.{Config, DefaultLog, Path}
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.{Attempt, ShiftApplication}
import net.shift.io.LocalFileSystem
import net.shift.loc.{Language, Loc}
import net.shift.security.{Permission, User}
import net.shift.server.http.{HttpProtocolBuilder, Request, TextHeader}
import net.shift.server.{Server, ServerConfig}
import net.shop.api.persistence.Persistence
import net.shop.mongodb.MongoDBPersistence
import net.shop.web.pages._
import net.shop.web.services._
import org.apache.log4j.PropertyConfigurator

object StartShop extends App with DefaultLog {

  implicit val fs = LocalFileSystem
  PropertyConfigurator.configure("config/log4j.properties")

  for {cfg <- Config.load(profile)} yield {

    val port = cfg.int("server.port")
    val dbPass = args.apply(0)

    implicit val c = cfg append Map("db.pwd" -> dbPass)

    log.info("Configs " + c.configs)

    Server(ServerConfig.fromConfig(c)).start(HttpProtocolBuilder(ShopApplication().shiftService))

    println("Server started on port " + port)
  }

  def profile = {
    val prof = System.getProperty("config.profile")
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

class ShopApplication(c: Config) extends ShiftApplication with ShopServices {
  self =>

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

  val pages = Pages(cfg, store)

  def servingRule = for {
    r <- withLanguage(Language("ro"))
    u <- user
    c <- staticFiles(Path("web/static")) |
      ajaxLogin |
      productsVariantImages |
      categoriesImages |
      logout |
      page("/", Path("web/index.html"), pages.catPage) |
      page("/terms", Path("web/terms.html"), pages.termsPage) |
      page("/dataprotection", Path("web/dataprotection.html"), pages.dataProtectionPage) |
      page("/returnpolicy", Path("web/returnpolicy.html"), pages.returnPolicyPage) |
      page("/cookies", Path("web/cookies.html"), pages.cookiesPage) |
      page("/aboutus", Path("web/aboutus.html"), pages.aboutUsPage) |
      page("/cart", Path("web/cart.html"), pages.cartPage, CartInfo(r, Nil)) |
      page("/newuser", Path("web/newuser.html"), pages.newUserPage) |
      xmlPage("/sitemap.xml", Path("web/sitemap.xml"), pages.siteMapPage) |
      settingsPage("/accountsettings", Path("web/accountsettings.html"), pages.accPage) |
      products(r) |
      product(r, u) |
      saveProduct(r, u) |
      saveCategory(r, u) |
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
      staticFile(Path("/google339a4b5281321c21.html"), "./web") |
      staticFile(Path("/googlef5775953f22a747b.html"), "./web") |
      notFoundService
    s <- refresh(c)

  } yield commonMeta(r, s)

  def commonMeta(req: Request, a: Attempt): Attempt = {
    import net.shift.engine._

    a.map{_.withResponse { r => r.withHeaders(
      TextHeader("Content-Language", req.language.toHttpString)
    )}}
  }

  def saveProduct(req: Request, u: Option[User]) = pageWithRules(Path("web/saveproduct.html"), pages.saveProductPage,
    for {
      _ <- get
      Path(_, _ :: "saveproduct" :: _) <- path
      _ <- userRequired(Loc.loc0(req.language)("login.fail").text)
      r <- permissions("Unauthorized", Permission("write"))
    } yield r, ProductPageState.build(req))

  def saveCategory(req: Request, u: Option[User]) = pageWithRules(Path("web/savecategory.html"), pages.saveCategoryPage,
    for {
      _ <- get
      Path(_, _ :: "savecategory" :: _) <- path
      _ <- userRequired(Loc.loc0(req.language)("login.fail").text)
      r <- permissions("Unauthorized", Permission("write"))
    } yield r, CategoryPageState.build(req))


  def product(req: Request, u: Option[User]) = pageWithRules(Path("web/product.html"), pages.prodDetailPage,
    for {
      _ <- get
      Path(_, _ :: "product" :: _) <- path
    } yield (), ProductPageState.build(req))

  def products(req: Request) = pageWithRules(Path("web/products.html"), pages.productsPage,
    for {
      _ <- get
      Path(_, _ :: "products" :: _) <- path
    } yield (), req)

}



