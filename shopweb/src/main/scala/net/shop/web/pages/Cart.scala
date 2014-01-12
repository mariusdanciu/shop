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
import scala.util.Try

trait Cart[T] extends DynamicContent[T] {

  def cartTemplate(state: T, r: Request): Try[NodeSeq] = for {
    input <- r.resource(Path("web/templates/cartpopup.html"))
    template <- XmlUtils.load(input)
  } yield new Html5(state, r.language, this)(Selectors.bySnippetAttr[SnipState[T]]).resolve(template)

}