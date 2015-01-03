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

trait SecuredService extends ShiftUtils {
  implicit def login(creds: Credentials): Option[User] = {
    creds match {
      case BasicCredentials("marius.danciu@gmail.com", "test") =>
        Some(User("marius.danciu@gmail.com", None, Set(Permission("read"), Permission("write"))))

      case BasicCredentials(email, password) =>

        ShopApplication.persistence.userByEmail(email) match {
          case Success(ud) if (ud.password == password) =>
            Some(User(ud.email, None, ud.permissions.map(Permission(_)).toSet))
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