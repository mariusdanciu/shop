package net.shop
package web.pages

import net.shift._
import net.shift._
import net.shift.common.Path
import net.shift.common.XmlUtils._
import net.shift.common.XmlUtils
import net.shift.engine.http.Request
import net.shift.engine.page.Html5
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import scala.xml.NodeSeq

trait Cart[T] extends DynamicContent[T] {

  def cartTemplate(state: T, r: Request): NodeSeq = {
    val template = XmlUtils.load(r.resource(Path("web/templates/cartpopup.html")))
    new Html5(state, this)(Selectors.bySnippetAttr[SnipState[T]]).resolve(template)
  }
}