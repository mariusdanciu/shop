package net.shop
package web.services

import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.MultiPartBody
import net.shift.engine.http.POST
import net.shift.engine.http.Resp
import net.shift.engine.utils.ShiftUtils
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet.inputFile
import net.shift.html.Formlet.inputText
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.Selectors
import net.shop.api.Category
import net.shop.web.ShopApplication
import net.shift.engine.http.DELETE
import net.shift.security.BasicCredentials
import net.shift.security.User
import net.shift.security.Permission
import net.shift.security.Credentials

object CategoryService extends PathUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  def deleteCategory = for {
    r <- DELETE
    Path("category" :: "delete" :: id :: Nil) <- path
    user <- auth
  } yield {
    ShopApplication.persistence.deleteCategories(id) match {
      case scala.util.Success(num) =>
        val deleted = scalax.file.Path.fromString(s"data/categories/$id").deleteRecursively(true);
        service(_(Resp.ok))
      case scala.util.Failure(t) => service(_(Resp.notFound))
    }
  }

  def updateCategory = for {
    r <- POST
    Path("category" :: "update" :: id :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, net.shift.html.Success(o)) =>
        val cpy = o.copy(id = Some(id), image = file.map(f => f._1))
        ShopApplication.persistence.updateCategories(cpy) match {
          case scala.util.Success(p) =>
            file.map { f =>
              scalax.file.Path.fromString(s"data/categories/${f._1}").write(f._2)
            }
            service(_(Resp.created))

          case scala.util.Failure(t) =>
            error("Cannot create category ", t)
            service(_(Resp.serverError))
        }

      case (_, net.shift.html.Failure(msgs)) => validationFail(msgs)

    }

  }

  def createCategory = for {
    r <- POST
    Path("category" :: "create" :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, net.shift.html.Success(o)) =>
        val cpy = o.copy(image = file.map(f => f._1))

        ShopApplication.persistence.createCategories(cpy) match {
          case scala.util.Success(p) =>
            file.map { f =>
              scalax.file.Path.fromString(s"data/categories/${p.head}/${f._1}").write(f._2)
            }
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create category ", t)
            service(_(Resp.serverError))
        }

      case (_, net.shift.html.Failure(msgs)) => validationFail(msgs)

    }

  }

  private def extract(implicit loc: Language, id: Option[String], multipart: MultiPartBody): (Option[(String, Array[Byte])], Validation[ValidationError, Category]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    val params = extractParams(text)
    val file = extractCategoryBin(bins.head)

    val category = ((net.shop.api.Category.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val categoryFormlet = Formlet(category) <*>
      inputText("title")(validateMapField("title", ?("title").text)) <*>
      inputFile("files")(validateDefault("files", None))

    (file, categoryFormlet validate params)

  }

}