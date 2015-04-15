package net.shop
package web.services

import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.BinaryPart
import net.shift.engine.http.MultiPartBody
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
import net.shift.engine.http.{ DELETE, GET, POST }
import net.shift.security.BasicCredentials
import net.shift.security.User
import net.shift.security.Permission
import net.shift.security.Credentials
import net.shop.model.ValidationFail
import net.shop.web.services.FormImplicits._
import net.shift.html.Invalid
import net.shift.html.Valid
import net.shift.engine.http.JsonResponse
import net.shop.api.Formatter
import net.shop.model.Formatters._
import net.shift.io.IODefaults
import net.shift.io.IO
import net.shift.io.FileSystem
import net.shift.io.FileOps
import net.shop.utils.ShopUtils._
import net.shop.api.ShopError

object CategoryService extends Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService
  with IODefaults {

  def getCategory(implicit fs: FileSystem) = for {
    r <- GET
    Path(_, "category" :: id :: Nil) <- path
    user <- auth
  } yield {
    ShopApplication.persistence.categoryById(id) match {
      case scala.util.Success(cat) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"))
        implicit val l = r.language
        service(_(JsonResponse(Formatter.format(cat))))
      case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(Resp.notFound))
    }
  }

  def deleteCategory(implicit fs: FileSystem) = for {
    r <- DELETE
    Path(_, "category" :: "delete" :: id :: Nil) <- path
    user <- auth
  } yield {
    ShopApplication.persistence.deleteCategories(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"));
        service(_(Resp.ok))
      case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(Resp.notFound))
    }
  }

  def updateCategory(implicit fs: FileSystem) = for {
    r <- POST
    Path(_, "category" :: "update" :: id :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, Valid(o)) =>
        val cpy = o.copy(id = Some(id))
        ShopApplication.persistence.updateCategories(cpy) match {
          case scala.util.Success(p) =>
            file.map { f =>
              IO.arrayProducer(f._2)(FileOps.writer(Path(s"${dataPath}/categories/${cpy.id.getOrElse("")}.png")))
            }
            service(_(Resp.created))

          case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            service(_(Resp.serverError.asText.withBody("category.create.fail")))
        }

      case (_, Invalid(msgs)) => 
        implicit val l = r.language
        validationFail(msgs)

    }

  }

  def createCategory = for {
    r <- POST
    Path(_, "category" :: "create" :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, Valid(o)) =>

        ShopApplication.persistence.createCategories(o) match {
          case scala.util.Success(p) =>
            file.map { f =>
              IO.arrayProducer(f._2)(FileOps.writer(Path(s"${dataPath}/categories/${p.head}.png")))
            }
            service(_(Resp.created))

          case scala.util.Failure(ShopError(msg, _)) => service(_(Resp.ok.asText.withBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create category ", t)
            service(_(Resp.serverError))
        }

      case (_, Invalid(msgs)) => 
        implicit val l = r.language
        validationFail(msgs)
    }

  }

  private def extract(implicit loc: Language, id: Option[String], multipart: MultiPartBody): (Option[(String, Array[Byte])], Validation[ValidationFail, Category]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    val params = extractParams(text)
    val file = extractCategoryBin(bins.head)

    val category = ((net.shop.api.Category.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val categoryFormlet = Formlet(category) <*>
      inputText("pos")(validateInt("pos", ?("list.pos").text)) <*>
      inputText("title")(validateMapField("title", ?("title").text))

    (file, categoryFormlet validate params)

  }

}