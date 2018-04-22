package net.shop.web.pages

import net.shift.common.{Path, ShiftFailure}
import net.shift.loc.Loc
import net.shift.server.http.Request
import net.shift.template.SnipState
import net.shift.template.Snippet.inline
import net.shop.model.{Category, ShopError}

import scala.util.{Failure, Success, Try}

/**
  * Created by mariu on 2/26/2017.
  */
trait SaveCategoryPage extends PageCommon[CategoryPageState] {
  self =>

  override def inlines = List(cat, path, fieldPrefix, method) ++ super.inlines

  val cat = inline[CategoryPageState]("cat") {
    s =>
      cateogry(s) match {
        case Success(p) =>
          val out = s.params.head match {
            case "title" => p.title_?(s.state.lang.name)
            case "pos" => p.position toString
          }
          Success((s.state.initialState.copy(product = Success(p)), out))
        case f =>
          Success((s.state.initialState, ""))
      }

  }

  val method = inline[CategoryPageState]("method") {
    s =>
      cateogry(s) match {
        case Success(p) => Success(s.state.initialState -> "PUT")
        case _ => Success(s.state.initialState -> "POST")
      }
  }

  val path = inline[CategoryPageState]("path") {
    s =>
      cateogry(s) match {
        case Success(p) => Success(s.state.initialState -> s"/categories/${p.stringId}")
        case _ => Success(s.state.initialState -> "/categories")
      }
  }
  val fieldPrefix = inline[CategoryPageState]("field_prefix") {
    s =>
      cateogry(s) match {
        case Success(p) => Success(s.state.initialState -> "edit")
        case _ => Success(s.state.initialState -> "create")
      }
  }

  def cateogry(s: SnipState[CategoryPageState]): Try[Category] = {
    s.state.initialState.product match {
      case Failure(t) =>
        Path(s.state.initialState.req.uri.path) match {
          case Path(_, _ :: _ :: id :: _) =>
            store.categoryById(id) match {
              case Failure(ShopError(msg, _)) => ShiftFailure(Loc.loc0(s.state.lang)(msg).text).toTry
              case Failure(t) => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
              case s => s
            }
          case _ => ShiftFailure(Loc.loc0(s.state.lang)("no.product").text).toTry
        }
      case s => s
    }
  }
}

object CategoryPageState {
  def build(req: Request): CategoryPageState = new CategoryPageState(req, Failure[Category](new RuntimeException("Category not found")))
}

case class CategoryPageState(req: Request, product: Try[Category])