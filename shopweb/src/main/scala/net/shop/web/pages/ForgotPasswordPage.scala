package net.shop.web.pages

import net.shift.template.DynamicContent
import net.shift.engine.http.Request
import net.shift.template.Snippet._
import net.shift.loc.Loc
import scala.util.Success
import net.shop.utils.ShopUtils
import net.shift.template._
import net.shift.template.Binds._
import net.shop.api.UserDetail

object ForgotPasswordPage extends DynamicContent[UserDetail] with ShopUtils  {
  override def snippets = List(pwd)

  val pwd = snip[UserDetail]("pwd") {
    s => Success((s.state.initialState, <b>{s.state.initialState.password}</b>))
  }

}