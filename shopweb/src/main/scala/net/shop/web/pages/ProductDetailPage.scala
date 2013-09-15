package net.shop
package web.pages

import net.shift.engine.http.Request
import net.shift.template.DynamicContent
import net.shift.template.Snippet.snip

object ProductDetailPage extends DynamicContent[Request] {

  def snippets = Nil

  def reqSnip(name: String) = snip[Request](name) _

}



