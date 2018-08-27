package net.shop
package web

import java.util.concurrent.Executors

import net.shift.common.{Config, DefaultLog, Path, State}
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.{Attempt, ShiftApplication}
import net.shift.io.LocalFileSystem
import net.shift.loc.{Language, Loc}
import net.shift.security.{Permission, User}
import net.shift.server.HttpServer
import net.shift.server.http.{Request, TextHeader}
import net.shop.persistence.Persistence
import net.shop.persistence.mongodb.MongoPersistence
import net.shop.web.pages._
import net.shop.web.services._
import org.apache.log4j.PropertyConfigurator

import scala.concurrent.ExecutionContext

object StartShop extends App with DefaultLog {

  System.setProperty("org.mongodb.async.type", "netty")

  implicit val fs = LocalFileSystem
  PropertyConfigurator.configure("config/log4j.properties")

  val s = for {cfg <- Config.load(profile)} yield {
    implicit val c = cfg

    log.info("Configs " + c.configs)

    implicit val ctx = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(c.int("server.numThreads", 10)))

    HttpServer(c, ShopApplication().shiftService).start().onFailure {
      case f => f.printStackTrace()
    }

    println("Server started.")


    //HttpsServer(c, ShopApplication().shiftService).start().onFailure{
    //  case f => f.printStackTrace()
    //}

    //println("SSL server started on port " + cfg.int("server.ssl.port"))

  }

  s recover {
    case t => t.printStackTrace()
  }

  def profile = {
    val prof = System.getProperty("config.profile")
    if (prof == null) {
      ""
    } else {
      prof
    }
  }

}

object ShopApplication {
  def apply()(implicit cfg: Config, ctx: ExecutionContext) = new ShopApplication(cfg)
}

class ShopApplication(c: Config)(implicit ctx: ExecutionContext) extends ShiftApplication with ShopServices {
  self =>

  implicit val cfg = c
  implicit val store: Persistence = MongoPersistence(c.string("db.url", ""))

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


  val pages = Pages(cfg, store)

  def servingRule = for {
    _ <- logRequest
    r <- withLanguage(Language("ro"))
    u <- user
    c <- staticFiles(Path("web/static")) |
      ajaxLogin |
      productsVariantImages |
      categoriesImages |
      logout |
      page("/login", Path("web/login.html"), pages.catPage) |
      page("/", Path("web/index.html"), pages.catPage) |
      page("/terms", Path("web/terms.html"), pages.termsPage) |
      page("/order_done", Path("web/order_done.html"), pages.termsPage) |
      page("/dataprotection", Path("web/dataprotection.html"), pages.dataProtectionPage) |
      page("/returnpolicy", Path("web/returnpolicy.html"), pages.returnPolicyPage) |
      page("/cookies", Path("web/cookies.html"), pages.cookiesPage) |
      page("/aboutus", Path("web/aboutus.html"), pages.aboutUsPage) |
      page("/cart", Path("web/cart.html"), pages.cartPage, CartInfo(r, Nil)) |
      page("/newuser", Path("web/newuser.html"), pages.newUserPage) |
      xmlPage("/sitemap.xml", Path("web/sitemap.xml"), pages.siteMapPage) |
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
      orderService.orderById |
      staticFile(Path("/google339a4b5281321c21.html"), "./web") |
      staticFile(Path("/googlef5775953f22a747b.html"), "./web") |
      notFoundService
    s <- refresh(c)

  } yield commonMeta(r, s)

  def commonMeta(req: Request, a: Attempt): Attempt = {
    import net.shift.engine._

    a.map {
      _.withResponse { r =>
        r.withHeaders(
          TextHeader("Content-Language", req.language.toHttpString)
        )
      }
    }
  }

  def saveProduct(req: Request, u: Option[User]): State[Request, Attempt] = pageWithRules(Path("web/saveproduct.html"), pages.saveProductPage,
    for {
      _ <- get
      Path(_, _ :: "saveproduct" :: _) <- path
      _ <- userRequired(Loc.loc0(req.language)("login.fail").text)
      r <- permissions("Unauthorized", Permission("write"))
    } yield r, ProductPageState.build(req))

  def saveCategory(req: Request, u: Option[User]): State[Request, Attempt] = pageWithRules(Path("web/savecategory.html"), pages.saveCategoryPage,
    for {
      _ <- get
      Path(_, _ :: "savecategory" :: _) <- path
      _ <- userRequired(Loc.loc0(req.language)("login.fail").text)
      r <- permissions("Unauthorized", Permission("write"))
    } yield r, CategoryPageState.build(req))


  def product(req: Request, u: Option[User]): State[Request, Attempt] = pageWithRules(Path("web/product.html"), pages.prodDetailPage,
    for {
      _ <- get
      Path(_, _ :: "product" :: _) <- path
    } yield (), ProductPageState.build(req))

  def products(req: Request): State[Request, Attempt] = pageWithRules(Path("web/products.html"), pages.categoryPage,
    for {
      _ <- get
      Path(_, _ :: "products" :: _) <- path
    } yield (), CategoryState(req))

}



