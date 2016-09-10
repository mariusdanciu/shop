package net.shop.web.pages

import net.shift.template.DynamicContent
import net.shift.template.Snippet._
import net.shift.loc.Loc
import scala.util.Success
import net.shift.template._
import net.shift.template.Binds._
import net.shop.api.UserDetail

object ForgotPasswordPage extends DynamicContent[UserDetail]  {
  override def snippets = List(pwd)

  val pwd = snip[UserDetail]("pwd") {
    s => Success((s.state.initialState, <b>{s.state.initialState.password}</b>))
  }

}