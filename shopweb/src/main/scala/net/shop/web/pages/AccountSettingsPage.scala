package net.shop.web.pages

import net.shift.loc.Loc
import net.shop.utils.ShopUtils
import net.shift.engine.http.Request
import scala.util.Success

object AccountSettingsPage extends Cart[Request] with ShopUtils { self =>

  override def snippets = List(title) ++ super.snippets

  val title = reqSnip("title") {
    s => Success((s.state.initialState, <h1>{ Loc.loc0(s.state.lang)("settings").text }</h1>))
  }

}
