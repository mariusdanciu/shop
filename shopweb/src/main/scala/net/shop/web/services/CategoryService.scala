package net.shop
package web.services

import net.shift.common._
import net.shift.engine.Attempt
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.HttpPredicates._
import net.shift.engine.http.{BinaryPart, MultiPartBody}
import net.shift.io.{FileSystem, IO, LocalFileSystem}
import net.shift.loc.{Language, Loc}
import net.shift.server.http.Responses.created
import net.shift.server.http.{Request, Responses}
import net.shop.model.{Category, FieldError, Formatter, ShopError}
import net.shop.utils.ShopUtils._
import net.shop.model.Formatters._

import scala.util.{Failure, Success}

trait CategoryService extends TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService
  with ServiceDependencies {

  def getCategory(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- get
    Path(_, _ :: "category" :: id :: Nil) <- path
    user <- auth
  } yield {
    store.categoryById(id) match {
      case Success(cat) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"))
        implicit val l = r.language
        service(_ (Responses.ok.withJsonBody(Formatter.format(cat))))
      case Failure(ShopError(msg, _)) =>
        service(_ (Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case Failure(t) =>
        service(_ (Responses.notFound))
    }
  }

  def deleteCategory(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- delete
    Path(_, _ :: "categories" :: id :: Nil) <- path
    user <- auth
  } yield {
    store.deleteCategory(id) match {
      case Success(num) =>
        fs.deletePath(Path(s"${dataPath}/categories/$id"));
        service(_ (Responses.ok))
      case Failure(ShopError(msg, _)) =>
        log.error(msg)
        service(_ (Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case Failure(t) =>
        log.error(t)
        service(_ (Responses.notFound))
    }
  }

  def updateCategory(implicit fs: FileSystem): State[Request, Attempt] = for {
    r <- put
    Path(_, _ :: "categories" :: id :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, "update_", Some(id), mp) match {
      case (file, Valid(o)) =>
        val cpy = o.copy(
          name = normalizeName(o.title_?(r.language.name))
        )
        store.updateCategory(cpy) match {
          case Success(p) =>
            file.map { f =>
              IO.arrayProducer(f._2)(LocalFileSystem.writer(Path(s"${dataPath}/categories/${cpy.id}.png")))
            }
            service(_ (Responses.created))
          case Failure(ShopError(msg, _)) =>
            service(_ (Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case Failure(t) =>
            service(_ (Responses.serverError.withTextBody(Loc.loc0(r.language)("category.create.fail").text)))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)

    }

  }

  def createCategory: State[Request, Attempt] = for {
    r <- post
    Path(_, _ :: "categories" :: Nil) <- path
    user <- auth
    mp <- multipartForm
  } yield {
    extract(r.language, "create_", None, mp) match {
      case (file, Valid(a)) =>
        val o = a.copy(
          name = normalizeName(a.title_?(r.language.name))
        )

        store.createCategory(o) match {
          case Success(p) =>
            file.map { f =>
              ImageUtils.resizeImage(234, f._2, s"${dataPath}/categories/${p}.png")
            }
            service(_ (created.withJsonBody("{\"href\": \"/\"}")))
          case Failure(ShopError(msg, _)) =>
            service(_ (Responses.ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case Failure(t) =>
            error("Cannot create category ", t)
            service(_ (Responses.serverError))
        }

      case (_, Invalid(msgs)) =>
        implicit val l = r.language
        validationFail(msgs)
    }

  }

  private def extract(implicit loc: Language, fieldPrefix: String, id: Option[String], multipart: MultiPartBody)
  : (Option[(String, Array[Byte])], Validation[Category, FieldError]) = {

    val (bins, text) = multipart.parts partition {
      case BinaryPart(h, content) => true
      case _ => false
    }

    val params = extractParams(text)
    val file = extractCategoryFile(bins.head)

    val category = ((net.shop.model.Category.apply _).curried) (id getOrElse store.makeID)
    val ? = Loc.loc0(loc) _

    val categoryFormlet = Validator(category) <*>
      Validator(validateDefault("")) <*>
      Validator(validateInt(fieldPrefix + "pos", ?("list.pos").text)) <*>
      Validator(validateMapField(fieldPrefix + "title", ?("title").text))

    val v = categoryFormlet validate params
    (file, v)

  }

}