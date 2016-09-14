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
import net.shift.engine._
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.page.Html5
import net.shift.io.IODefaults
import net.shift.loc.Loc
import net.shift.security.User
import net.shift.template.DynamicContent
import net.shift.template.PageState
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
import net.shift.common.ShiftFailure
import net.shift.engine.http.HttpPredicates._
import net.shift.template.Template._
import net.shift.http.TextHeader
import net.shift.engine.Attempt
import net.shift.http.Responses._
import net.shift.http.ContentType._
import net.shift.http.HTTPRequest
import net.shift.http.Responses._
import net.shop.web.pages.SettingsPageState
import net.shop.web.pages.CartState
import net.shift.http.ContentType._
import net.shop.utils.ShopUtils

trait ShopServices extends TraversingSpec
    with DefaultLog
    with SecuredService
    with ServiceDependencies { self =>

  def notFoundService = for {
    r <- req
  } yield {
    r.header("X-Requested-With") match {
      case Some(TextHeader(_, "XMLHttpRequest")) =>
        service(_(notFound))
      case _ =>
        if (r.uri.path.startsWith("/static"))
          service(_(notFound))
        else
          service(_(redirect("/")))
    }
  }

  def ajaxLogin = for {
    _ <- ajax
    r <- path("auth")
    u <- authenticate(Loc.loc0(r.language)("login.fail").text, 406)
  } yield service(_(ok.withSecurityCookies(u)))

  def tryLogout(r: HTTPRequest, attempt: Attempt) = State.put[HTTPRequest, Attempt] {
    val logout = !r.uri.param("logout").isEmpty
    if (logout)
      attempt.map(_.withResponse { _ dropSecurityCookies })
    else
      attempt
  }

  def refresh(attempt: Attempt) = for {
    u <- user
    serv <- State.put[HTTPRequest, Attempt](attempt)
  } yield {
    u match {
      case Some(usr) => serv.map(_.withResponse { _ withSecurityCookies (usr) })
      case _         => serv
    }
  }

  def pageWithRules[S, T](filePath: Path,
                          snipets: DynamicContent[S],
                          rules: State[HTTPRequest, T],
                          initialState: S): State[HTTPRequest, Attempt] = {
    for {
      _ <- rules
      r <- req
      u <- user
    } yield {
      val logout = !r.uri.param("logout").isEmpty
      Html5.pageFromFile(PageState(initialState, r.language, if (logout) None else u), filePath, snipets)
    }

  }

  def page(uri: String, filePath: Path, snipets: DynamicContent[HTTPRequest]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.uri.param("logout").isEmpty
    Html5.pageFromFile(PageState(r, r.language, if (logout) None else u), filePath, snipets)
  }

  def page[T](uri: String, filePath: Path, snipets: DynamicContent[T], initialState: T) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.uri.param("logout").isEmpty
    Html5.pageFromFile(PageState(initialState, r.language, if (logout) None else u), filePath, snipets)
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

  def page[T](f: (HTTPRequest, Option[User]) => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
    u <- user
  } yield {
    val logout = !r.uri.param("logout").isEmpty
    Html5.pageFromFile(PageState(f(r, u), r.language, if (logout) None else u), filePath, snipets)
  }

  def productsVariantImages = for {
    Path(_, _ :: "data" :: "products" :: id :: variant :: file :: Nil) <- path
    r <- fileAsResponse(Path(s"${ShopUtils.dataPath}/products/$id/$variant/$file"), ImagePng)
  } yield service(resp =>
    resp(r.withHeaders(TextHeader("cache-control", "max-age=86400"))))

  def categoriesImages = for {
    Path(_, _ :: "data" :: "categories" :: file :: Nil) <- path
    r <- fileAsResponse(Path(s"${ShopUtils.dataPath}/categories/$file"), ImagePng)
  } yield service(resp =>
    resp(r.withHeaders(TextHeader("cache-control", "max-age=86400"))))

  def getCart() = for {
    r <- req
    lang <- language
    Path(_, _ :: "getcart" :: Nil) <- path
  } yield service(resp =>
    r.cookie("cart") match {
      case Some(c) => {
        implicit val formats = DefaultFormats

        listTraverse.sequence(for {
          (item, index) <- readCart(c.cookieValue).items.zipWithIndex
          prod <- store.productById(item.id).toOption
        } yield {
          Html5.runPageFromFile(PageState(CartState(index, item, prod), r.language), Path("web/templates/cartitem.html"), CartItemNode).map(_._2 toString)
        }) match {
          case Success(list)                         => resp(ok.withJsonBody(write(list)))
          case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case Failure(t) =>
            log.error("Failed processing cart ", t)
            resp(ok.withJsonBody(write(Nil)))
        }

      }
      case _ => resp(ok.withJsonBody("[]"))
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

}




