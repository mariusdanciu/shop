package net.shop
package web.services

import scala.Option.option2Iterable
import scala.util.Failure
import scala.util.Success
import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import net.shift.common.Config
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.State
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils
import net.shift.io.IODefaults
import net.shift.loc.Loc
import net.shift.security.User
import net.shift.template.DynamicContent
import net.shift.template.PageState
import net.shift.template.Selectors
import net.shop.api.Cart
import net.shop.api.ShopError
import net.shop.tryApplicative
import net.shop.web.pages.AccountSettingsPage
import net.shop.web.pages.AccountSettingsPage
import net.shop.web.pages.CartItemNode
import net.shop.web.pages.CartState
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.ProductDetailPage
import net.shop.web.pages.ProductPageState
import net.shop.web.pages.ProductsPage
import net.shop.web.pages.SettingsPageState
import utils.ShopUtils._
import net.shift.engine.http.RequestShell
import net.shift.common.ShiftFailure

trait ShopServices extends ShiftUtils
    with Selectors
    with TraversingSpec
    with DefaultLog
    with SecuredService
    with IODefaults
    with ServiceDependencies { self =>

  def notFoundService = for {
    r <- req
  } yield {
    r.header("X-Requested-With") match {
      case Some(HeaderKeyValue(_, "XMLHttpRequest")) =>
        service(_(Resp.ok))
      case _ => service(_(Resp.redirect("/")))
    }
  }

  implicit val reqSelector = bySnippetAttr[Request]
  implicit val cartItemsSelector = bySnippetAttr[CartState]
  implicit val settingsSelector = bySnippetAttr[SettingsPageState]

  def mobileUA: State[Request, Request] =
    for {
      r <- req if {
        val isMobile = r.header("User-Agent").map { h => h.value.contains("mobile") } getOrElse false
        val mobileURI = r.uri.startsWith("/mobile")
        isMobile && !mobileURI
      }
    } yield {
      r
    }

  def ajaxLogin = for {
    _ <- ajax
    r <- path("auth")
    u <- authenticate(Loc.loc0(r.language)("login.fail").text, 406)
  } yield service(_(Resp.ok.securityCookies(u)))

  def ajaxProductsList = for {
    r <- ajax
    p <- page("products", Path("web/templates/productslist.html"), new ProductsPage() {
      val cfg = self.cfg
      val store = self.store
    })
  } yield p

  def ajaxCategoriesList = for {
    r <- ajax
    p <- page("", Path("web/templates/categorieslist.html"), new CategoryPage() {
      val cfg = self.cfg
      val store = self.store
    })
  } yield p

  def ajaxProductDetail = for {
    r <- ajax
    p <- page(ProductPageState.build _, "productquickview", Path("web/templates/productquickview.html"), new ProductDetailPage() {
      val cfg = self.cfg
      val store = self.store
    })
  } yield p

  def ajaxUsersView = for {
    r <- ajax
    p <- usersPage("usersview", Path("web/templates/users.html"), new AccountSettingsPage() {
      val cfg = self.cfg
      val store = self.store
    })
  } yield p

  def ajaxOrdersView = for {
    r <- ajax
    p <- settingsPage("ordersview", Path("web/templates/ordersview.html"), new AccountSettingsPage() {
      val cfg = self.cfg
      val store = self.store
    })
  } yield p

  def tryLogout(r: Request, attempt: Attempt) = State.put[Request, Attempt] {
    val logout = !r.param("logout").isEmpty
    if (logout)
      attempt.map(_.withResponse { _ dropSecurityCookies })
    else
      attempt
  }

  def refresh(attempt: Attempt) = for {
    u <- user
    serv <- State.put[Request, Attempt](attempt)
  } yield {
    u match {
      case Some(usr) => serv.map(_.withResponse { _ securityCookies (usr) })
      case _         => serv
    }
  }

  def page[T](uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.param("logout").isEmpty
    Html5.pageFromFile(PageState(r, r.language, if (logout) None else u), filePath, snipets)
  }

  def mobilePage[T](uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri) | mobileUA
    u <- user
  } yield {
    val logout = !r.param("logout").isEmpty
    Html5.pageFromFile(PageState(r, r.language, if (logout) None else u), filePath, snipets)
  }

  def staticTextFiles[T](paths: String*) = for {
    p <- path
    input <- fileOf(Path(s"web/$p"))
  } yield {
    service(_(TextResponse(input)))
  }

  def settingsPage(uri: String, filePath: Path, snipets: DynamicContent[SettingsPageState]) = for {
    r <- req
    _ <- startsWith(Path(uri))
    u <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    Html5.pageFromFile(PageState(SettingsPageState(r, None), r.language, Some(u)), filePath, snipets)
  }

  def usersPage(uri: String, filePath: Path, snipets: DynamicContent[SettingsPageState]) = for {
    r <- req
    _ <- path(uri)
    u <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    Html5.pageFromFile(PageState(SettingsPageState(r, None), r.language, Some(u)), filePath, snipets)
  }

  def page[T](f: (Request, Option[User]) => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.param("logout").isEmpty
    Html5.pageFromFile(PageState(f(r, u), r.language, if (logout) None else u), filePath, snipets)(bySnippetAttr[T], fs)
  }

  def productsVariantImages = for {
    Path(_, "data" :: "products" :: id :: variant :: file :: Nil) <- path
    input <- fileOf(Path(s"${dataPath}/products/$id/$variant/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/jpg").withHeaders(Header("cache-control", "max-age=86400"))))

  def categoriesImages = for {
    Path(_, "data" :: "categories" :: file :: Nil) <- path
    input <- fileOf(Path(s"${dataPath}/categories/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/png").withHeaders(Header("cache-control", "max-age=86400"))))

  def categoriesMobileImages = for {
    Path(_, "data" :: "categories" :: "mobile" :: file :: Nil) <- path
    input <- fileOf(Path(s"${dataPath}/categories/mobile/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/png").withHeaders(Header("cache-control", "max-age=86400"))))

  def getCart() = for {
    r <- req
    lang <- language
    Path(_, "getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        implicit def snipsSelector[T] = bySnippetAttr[T]

        listTraverse.sequence(for {
          (item, index) <- readCart(c.value).items.zipWithIndex
          prod <- store.productById(item.id).toOption
        } yield {
          Html5.runPageFromFile(PageState(CartState(index, item, prod), r.language), Path("web/templates/cartitem.html"), CartItemNode).map(_._2 toString)
        }) match {
          case Success(list)                         => resp(JsonResponse(write(list)))
          case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
          case Failure(t) =>
            log.error("Failed processing cart ", t)
            resp(JsonResponse(write(Nil)))
        }

      }
      case _ => resp(JsonResponse("[]"))
    })

  private def readCart(json: String): Cart = {
    implicit val formats = DefaultFormats
    parse(java.net.URLDecoder.decode(json, "UTF-8")).extract[Cart]
  }

  def createProduct = new ProductService() {
    val cfg = self.cfg
    val store = self.store
  }.createProduct

  def updateProduct = new ProductService() {
    val cfg = self.cfg
    val store = self.store
  }.updateProduct

  def deleteProduct = new ProductService() {
    val cfg = self.cfg
    val store = self.store
  }.deleteProduct

  def createCategory = new CategoryService() {
    val cfg = self.cfg
    val store = self.store
  }.createCategory

  def deleteCategory = new CategoryService() {
    val cfg = self.cfg
    val store = self.store
  }.deleteCategory

  def updateCategory = new CategoryService() {
    val cfg = self.cfg
    val store = self.store
  }.updateCategory

  private val mobileNames = Set("Android", "webOS", "iPhone", "iPad", "iPod", "BlackBerry", "Windows Phone")

  def toMobileIfNeeded = State.state[Request, Attempt] {
    r =>
      val ua = r.header("User-Agent").map { _ value } getOrElse ""
      val b = (for { n <- mobileNames } yield ua.contains(n)).find(x => x).getOrElse(false)
      r.path.parts match {
        case Nil if b =>
          Success((r, service(_(Resp.redirect("/mobile")))))
        case _ =>
          ShiftFailure.toTry
      }
  }

}




