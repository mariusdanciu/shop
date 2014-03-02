package net.shop
package web.pages

import net.shift._
import net.shift._
import net.shift.common.Path
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import scala.xml.NodeSeq
import scala.util.Try
import net.shift.common.XmlUtils
import net.shop.web.form.OrderForm

trait Cart[T] extends DynamicContent[T] with XmlUtils with Selectors {

  def snippets = List(order)

  def cartTemplate(state: T, r: Request): Try[NodeSeq] = for {
    input <- r.resource(Path("web/templates/cartpopup.html"))
    template <- load(input)
  } yield new Html5(state, r.language, this)(bySnippetAttr[SnipState[T]]).resolve(template)

  def order = snip[T]("order") {
    s =>
      bind(s.node) {
        case "form" > (a / _) => <form id="order_form">{ OrderForm.form(s.language).html }</form>
      } map ((s.state, _))
  }

  def searchTemplate(state: T, r: Request): Try[NodeSeq] = for {
    input <- r.resource(Path("web/templates/search.html"))
    template <- load(input)
  } yield new Html5(state, r.language, this)(bySnippetAttr[SnipState[T]]).resolve(template)

}