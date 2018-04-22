package net.shop.web.pages

import net.shift.template.DynamicContent
import net.shift.template.Snippet._
import net.shop.model.UserDetail

import scala.util.Success

object ForgotPasswordPage extends DynamicContent[UserDetail]  {
  override def snippets = List(pwd)

  val pwd = snip[UserDetail]("pwd") {
    s => Success((s.state.initialState, <b>{s.state.initialState.password}</b>))
  }

}