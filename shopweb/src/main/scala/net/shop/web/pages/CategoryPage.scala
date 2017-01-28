package net.shop
package web.pages

import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shift.loc._
import net.shift.server.http.Request
import net.shift.template.Binds._
import net.shop.api.ShopError
import net.shop.utils.ShopUtils._
import net.shop.web.services.ServiceDependencies

import scala.util.{Failure, Success}
import scala.xml._

trait CategoryPage extends PageCommon[Request] with ServiceDependencies { self =>

  val item = reqSnip("item") {
    s =>
      {
        val prods = store.allCategories match {
          case Success(list) =>
            val items = list flatMap { cat =>
              bind(s.node) {
                case Xml("a", a, childs) => <a href={s"/products/${itemToPath(cat)}"}>
                  {childs}
                </a> % a
                case Xml("img", a, childs) => <img id={ cat stringId }/> % (a + ("src", categoryImagePath(cat)))
                case Xml("h3", a, _)       => <h3>{ cat.title_?(s.state.lang.name) }</h3> % a
              } match {
                case Success(n)                 => n
                case Failure(ShopError(msg, _)) => errorTag(Loc.loc0(s.state.lang)(msg).text)
                case Failure(f)                 => errorTag(f toString)
              }
            } toList

            items.grouped(3).map { l => <div class="row">{ NodeSeq.fromSeq(l) }</div> }
          case Failure(t) => <div class="error">{ Loc.loc0(s.state.lang)("no.categories").text }</div>
        }

        Success(s.state.initialState, prods.toSeq)
      }
  }

  override def snippets = List(item) ++ super.snippets

}

