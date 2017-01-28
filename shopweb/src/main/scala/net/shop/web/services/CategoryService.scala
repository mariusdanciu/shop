package net.shop
package web.services

import net.shift.common._
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.http.{BinaryPart, MultiPartBody}
import net.shift.io.{FileSystem, IO, LocalFileSystem}
import net.shift.loc.{Language, Loc}
import net.shift.server.http.Responses
import net.shop.api.{Category, Formatter, ShopError}
import net.shop.model.FieldError
import net.shop.model.Formatters.CategoryWriter
import net.shop.utils.ShopUtils.dataPath

trait CategoryService extends TraversingSpec
    with DefaultLog
    with FormValidation
    with SecuredService
    with ServiceDependencies {

  def getCategory(implicit fs: FileSystem) = for {
    r <- get
    Path(_, "category" :: id :: Nil) <- path
    user <- auth
  } yield {
    store.categoryById(id) match {
      case scala.util.Success(cat) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"))
        implicit val l = r.language
        service(_(Responses.ok.withJsonBody(Formatter.format(cat))))
      case scala.util.Failure(ShopError(msg, _)) => service(_(Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(Responses.notFound))
    }
  }

  def deleteCategory(implicit fs: FileSystem) = for {
    r <- delete
    Path(_, "category" :: "delete" :: id :: Nil) <- path
    user <- auth
  } yield {
    store.deleteCategories(id) match {
      case scala.util.Success(num) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"));
        service(_(Responses.ok))
      case scala.util.Failure(ShopError(msg, _)) => service(_(Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case scala.util.Failure(t)                 => service(_(Responses.notFound))
    }
  }

  def updateCategory(implicit fs: FileSystem) = for {
    r <- post
    Path(_, "category" :: "update" :: id :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, Valid(o)) =>
        val cpy = o.copy(id = Some(id))
        store.updateCategories(cpy) match {
          case scala.util.Success(p) =>
            file.map { f =>
              IO.arrayProducer(f._2)(LocalFileSystem.writer(Path(s"${dataPath}/categories/${cpy.id.getOrElse("")}.png")))
            }
            service(_(Responses.created))

          case scala.util.Failure(ShopError(msg, _)) => service(_(Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            service(_(Responses.serverError.withTextBody(Loc.loc0(r.language)("category.create.fail").text)))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)

    }

  }

  def createCategory = for {
    r <- post
    Path(_, "category" :: "create" :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, None, mp) match {
      case (file, Valid(o)) =>

        store.createCategories(o) match {
          case scala.util.Success(p) =>
            file.map { f =>
              IO.arrayProducer(f._2)(LocalFileSystem.writer(Path(s"${dataPath}/categories/${p.head}.png")))
            }
            service(_(Responses.created))

          case scala.util.Failure(ShopError(msg, _)) => service(_(Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create category ", t)
            service(_(Responses.serverError))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)
    }

  }

  private def extract(implicit loc: Language, id: Option[String], multipart: MultiPartBody): (Option[(String, Array[Byte])], Validation[Category, FieldError]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _                      => false
    }

    val params = extractParams(text)
    val file = extractCategoryBin(bins.head)

    val category = ((net.shop.api.Category.apply _).curried)(id)
    val ? = Loc.loc0(loc) _

    val categoryFormlet = Validator(category) <*>
      Validator(validateDefault("")) <*>
      Validator(validateInt("pos", ?("list.pos").text)) <*>
      Validator(validateMapField("title", ?("title").text))

    val v = categoryFormlet validate params
    (file, v)

  }

}