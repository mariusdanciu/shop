package net.shop
package web.services

import net.shift.security.BasicCredentials
import net.shift.security.User
import net.shift.security.Permission
import net.shift.security.Credentials
import net.shift.loc.Loc
import net.shift.engine.utils.ShiftUtils
import net.shift.engine.http.Request

trait SecuredService extends ShiftUtils {
  implicit def login(creds: Credentials): Option[User] = {
    creds match {
      case BasicCredentials("marius", "boot") => Some(User("marius", None, Set(Permission("write"))))
      case _                                  => None
    }
  }

  def auth = for {
    r <- req
    user <- authenticate(Loc.loc0(r.language)("login.fail").text)
  } yield user
}