package net.shop
package web.pages

import scala.xml._
import scala.xml._
import net.shift._
import net.shift._
import net.shift.engine.http._
import net.shift.engine.http._
import net.shift.loc._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Snippet._
import net.shop.web.ShopApplication
import scala.util.Success
import scala.util.Failure
import net.shop.utils.ShopUtils

object TermsPage extends Cart[Request] with ShopUtils { self =>

  override def snippets = List(title) ++ super.snippets

  val title = reqSnip("title") {
    s => Success((s.state.initialState, <h1>{ Loc.loc0(s.state.lang)("terms.link").text }</h1>))
  }

}
