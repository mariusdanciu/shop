package net.shop
package web.pages

import scala.util.Failure
import scala.util.Success
import scala.xml._
import scala.xml._
import net.shift._
import net.shift._
import net.shift.engine.http._
import net.shift.loc._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shop.utils.ShopUtils._
import net.shop.web.ShopApplication
import net.shop.api.ShopError
import net.shift.common.Xml
import net.shift.common.XmlImplicits._
import net.shop.web.services.ServiceDependencies

trait CategoryPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List(item) ++ super.snippets

  val item = reqSnip("item") {
    s =>
      {
        val prods = store.allCategories match {
          case Success(list) =>
            val items = list flatMap { cat =>
              (bind(s.node) {
                case Xml("a", a, childs)                            => <a href={ s"products?cat=${cat.stringId}" }>{ childs }</a> % a
                case Xml("div", HasClass("fh5co-cover", a), childs) => <div id={ cat stringId } style={ "background: url('" + categoryImagePath(cat) + "') no-repeat" }>{ childs }</div> % a
                case Xml("h3", a, _)                                => <h3>{ cat.title_?(s.state.lang.name) }</h3> % a
              }) match {
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

}

