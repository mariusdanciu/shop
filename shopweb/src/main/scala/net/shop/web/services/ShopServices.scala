package net.shop
package web.services

import net.shift.common._
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.page.Html5
import net.shift.engine.{Attempt, _}
import net.shift.loc.Loc
import net.shift.security.User
import net.shift.server.http.ContentType._
import net.shift.server.http.Responses._
import net.shift.server.http.{ContentType, Param, Request, ResponseFunc, TextHeader}
import net.shift.template.Template._
import net.shift.template.{DynamicContent, PageState}
import net.shop.api.persistence.Persistence
import net.shop.api.{Cart, ShopError}
import net.shop.utils.ShopUtils
import net.shop.web.pages.{CartItemNode, CartState}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.{DefaultFormats, jvalue2extractable, string2JsonInput}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ShopServices extends TraversingSpec
  with DefaultLog
  with SecuredService
  with ServiceDependencies {
  self =>

  val cartItemNode = new CartItemNode {
    val store = self.store
    val sf = self.fs
    val cfg = self.cfg
  }

  def notFoundService: State[Request, Attempt] = for {
    r <- req
  } yield {
    r.header("X-Requested-With") match {
      case Some(TextHeader(_, "XMLRequest")) =>
        service(_ (notFound))
      case _ =>
        if (r.uri.path.startsWith("/static"))
          service(_ (notFound))
        else
          service(_ (redirect("/")))
    }
  }

  def ajaxLogin: State[Request, Attempt] = for {
    r <- req
    _ <- ajax
    r <- path("/auth")
    u <- authenticate(Loc.loc0(r.language)("login.fail").text, 406)
  } yield service(_ (ok.withSecurityCookies(u)))

  def logRequest: State[Request, Request] = for {
    r <- req
  } yield {
    log.debug(s"Request ${r.method} ${r.uri.path}")
    r
  }


  def logout: State[Request, Attempt] = for {
    r <- path("/logout")
  } yield service(_ (redirect("/").dropSecurityCookies))

  def refresh(attempt: Attempt): State[Request, Attempt] = {
    for {
      r <- req
      u <- user
      serv <- State.put[Request, Attempt](attempt)
    } yield {
      u match {
        case Some(usr) =>
          serv.map(_.withResponse { r =>
            val isLogout = r.cookie("identity").map { c => c.maxAge == Some(0) } getOrElse false

            if (isLogout)
              r
            else
              r.withSecurityCookies(usr)
          })
        case _ => serv
      }
    }
  }

  def pageWithRules[S, T](filePath: Path,
                          snipets: DynamicContent[S],
                          rules: State[Request, T],
                          initialState: S): State[Request, Attempt] = {
    for {
      _ <- rules
      r <- req
      u <- user
    } yield {
      Html5.pageFromFile(PageState(initialState, r.language, u), filePath, snipets)
    }

  }

  def xmlPage(uri: String, filePath: Path, snippets: DynamicContent[Request]): State[Request, Try[(ResponseFunc) => Unit]] = for {
    attempt <- page(uri, filePath, snippets)
  } yield {
    attempt.map(_.withResponse { r => r.withMime(ContentType.TextXml) })
  }

  def page(uri: String, filePath: Path, snipets: DynamicContent[Request]): State[Request, Attempt] = for {
    r <- path(uri)
    u <- user
  } yield {
    Html5.pageFromFile(PageState(r, r.language, u), filePath, snipets)
  }

  def page[T](uri: String, filePath: Path, snipets: DynamicContent[T], initialState: T) = for {
    r <- path(uri)
    u <- user
  } yield {
    Html5.pageFromFile(PageState(initialState, r.language, u), filePath, snipets)
  }

  def page[T](f: (Request, Option[User]) => T, uri: String, filePath: Path, snipets: DynamicContent[T]) = for {
    r <- path(uri)
    u <- user
  } yield {
    Html5.pageFromFile(PageState(f(r, u), r.language, u), filePath, snipets)
  }

  def productsVariantImages: State[Request, Attempt] = for {
    Path(_, _ :: "data" :: "products" :: id :: variant :: file :: Nil) <- path
    r <- fileAsResponse(Path(s"${ShopUtils.dataPath}/products/$id/$variant/$file"), ImagePng)
  } yield service(resp =>
    resp(r.withHeaders(TextHeader("cache-control", "max-age=86400"))))

  def categoriesImages: State[Request, Attempt] = for {
    Path(_, _ :: "data" :: "categories" :: file :: Nil) <- path
    r <- fileAsResponse(Path(s"${ShopUtils.dataPath}/categories/$file"), ImagePng)
  } yield service(resp =>
    resp(r.withHeaders(TextHeader("cache-control", "max-age=86400"))))

  def getCart(): State[Request, Attempt] = for {
    r <- req
    lang <- language
    Path(_, _ :: "getcart" :: Nil) <- path
  } yield service { resp =>

    r.uri.param("cart") match {
      case Some(Param(_, enc :: _)) => {


        implicit val formats = DefaultFormats
        val dec = Base64.decodeString(enc)
        log.debug(s"Cart content $dec")

        listTraverse.sequence(for {
          (item, index) <- readCart(dec).items.zipWithIndex
        } yield {
          store.productById(item.id).flatMap { prod =>
            Html5.runPageFromFile(PageState(CartState(index, item, prod), r.language), Path("web/templates/cartitem.html"), cartItemNode).map(_._2 toString)
          }
        }).map {
          list => resp(ok.withJsonBody(write(list)))
        } recover {
          case ShopError(msg, _) => service(_ (ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case t =>
            log.error("Failed processing cart ", t)
            resp(ok.withJsonBody(write(Nil)))
        }

      }
      case _ => resp(ok.withJsonBody("[]"))
    }
  }

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

  def siteMap(store: Persistence) = for {
    PathObj(_, _ :: "sitemap.xml" :: Nil) <- path
  } yield {


  }

}




