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

import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.AsyncResponse
import net.shift.engine.http.Attempt
import net.shift.engine.http.ImageResponse
import net.shift.engine.http.JsonResponse
import net.shift.engine.http.Request
import net.shift.engine.http.Resp
import net.shift.engine.http.Response.augmentResponse
import net.shift.engine.http.TextResponse
import net.shift.engine.http.serviceServiceUtils
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils
import net.shift.loc.Loc
import net.shift.security.BasicCredentials
import net.shift.security.Credentials
import net.shift.security.Permission
import net.shift.security.User
import net.shift.template.DynamicContent
import net.shift.template.PageState
import net.shift.template.Selectors
import net.shift.template.SnipState
import net.shop.api.Cart
import net.shop.tryApplicative
import net.shop.web.ShopApplication
import net.shop.web.pages.CartItemNode
import net.shop.web.pages.CartState
import net.shop.web.pages.CategoryPage
import net.shop.web.pages.ProductDetailPage
import net.shop.web.pages.ProductPageState
import net.shop.web.pages.ProductsPage

trait ShopServices extends PathUtils with ShiftUtils with Selectors with TraversingSpec with DefaultLog with SecuredService {

  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }

  implicit val reqSelector = bySnippetAttr[SnipState[Request]]
  implicit val cartItemsSelector = bySnippetAttr[SnipState[CartState]]

  def logReq = for {
    r <- req
  } yield {
    log.debug("Request: " + r.uri)
    r
  }
  def ajaxLogin = for {
    _ <- ajax
    r <- path("auth")
    u <- authenticate(Loc.loc0(r.language)("login.fail").text)
  } yield service(_(Resp.ok.securityCookies(u)))

  def ajaxProductsList = for {
    r <- ajax
    p <- page("products", Path("web/templates/productslist.html"), ProductsPage)
  } yield p

  def ajaxCategoriesList = for {
    r <- ajax
    p <- page("/", Path("web/templates/categorieslist.html"), CategoryPage)
  } yield p

  def ajaxProductDetail = for {
    r <- ajax
    p <- page(ProductPageState.build _, "productquickview", Path("web/templates/productquickview.html"), ProductDetailPage)
  } yield p

  def tryLogout(r: Request, attempt: Attempt): Attempt = {
    val logout = !r.param("logout").isEmpty
    if (logout)
      attempt.map(_.withResponse { _ dropSecurityCookies })
    else
      attempt
  }

  def page[T](uri: String, filePath: Path, snipets: DynamicContent[Request]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.param("logout").isEmpty
    tryLogout(r, Html5.pageFromFile(PageState(r, r.language, if (logout) None else u), filePath, snipets))
  }

  def page[T](f: (Request, Option[User]) => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.param("logout").isEmpty
    tryLogout(r, Html5.pageFromFile(PageState(f(r, u), r.language, if (logout) None else u), filePath, snipets)(bySnippetAttr[SnipState[T]]))
  }

  def productsVariantImages = for {
    Path("data" :: "products" :: id :: variant :: file :: Nil) <- path
    input <- fileOf(Path(s"data/products/$id/$variant/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/jpg")))

  def categoriesImages = for {
    Path("data" :: "categories" :: id :: file :: Nil) <- path
    input <- fileOf(Path(s"data/categories/$id/$file"))
  } yield service(resp =>
    resp(new ImageResponse(input, "image/png")))

  def getCart() = for {
    r <- req
    lang <- language
    Path("getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats
        implicit def snipsSelector[T] = bySnippetAttr[SnipState[T]]
        listTraverse.sequence(for {
          item <- readCart(c.value).items
          prod <- ShopApplication.persistence.productById(item.id).toOption
        } yield {
          Html5.runPageFromFile(PageState(CartState(item.count, prod), r.language), Path("web/templates/cartitem.html"), CartItemNode).map(_._2 toString)
        }) match {
          case Success(list) => resp(JsonResponse(write(list)))
          case Failure(t) =>
            log.error("Failed processing cart ", t)
            resp(JsonResponse(write(Nil)))
        }

      }
      case _ => resp(JsonResponse("[]"))
    })

  def orderService = OrderService

  private def readCart(json: String): Cart = {
    implicit val formats = DefaultFormats
    parse(java.net.URLDecoder.decode(json, "UTF-8")).extract[Cart]
  }

  def createProduct = ProductService.createProduct

  def updateProduct = ProductService.updateProduct

  def deleteProduct = ProductService.deleteProduct

  def createCategory = CategoryService.createCategory

  def deleteCategory = CategoryService.deleteCategory

  def updateCategory = CategoryService.updateCategory

  def createUser = UserService.createUser

  def forgotPassword = UserService.forgotPassword

  def updateSettings = SettingsService.updateSettings

}


