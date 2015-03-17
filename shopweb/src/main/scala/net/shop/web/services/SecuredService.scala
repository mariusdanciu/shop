package net.shop
package web.services

import net.shift.security.BasicCredentials
import net.shift.security.User
import net.shift.security.Permission
import net.shift.security.Credentials
import net.shift.loc.Loc
import net.shift.engine.utils.ShiftUtils
import net.shift.engine.http.Request
import net.shop.web.ShopApplication
import scala.util.Success
import net.shift.common.Config
import net.shift.io.IODefaults

trait SecuredService extends ShiftUtils with IODefaults {
  implicit def login(creds: Credentials): Option[User] = {
    creds match {
      case BasicCredentials(email, password) =>

        ShopApplication.persistence.userByEmail(email) match {
          case Success(Some(ud)) if (ud.password == password) =>
            val extraPerms = if (ud.email == Config.string("admin.user")) List("write") else Nil
            Some(User(ud.email, None, (ud.permissions ++ extraPerms).map(Permission(_)).toSet))
          case _ =>
            None
        }
      case _ => None
    }
  }

  def auth = for {
    r <- req
    user <- authenticate(Loc.loc0(r.language)("login.fail").text)
  } yield user
}