package net.shop
package web.services

import net.shift.common.ShiftFailure
import net.shift.engine.http.HttpPredicates._
import net.shift.loc.Loc
import net.shift.security._

import scala.util.{Success, Try}

trait SecuredService extends ServiceDependencies {

  implicit def login(creds: Credentials): Try[User] = {
    creds match {
      case BasicCredentials(email, password) =>

        val admins = cfg.list("admin.users") toSet

        if (admins.contains(email)) {
          Success(User(email, Some(Organization("idid")), Set(Permission("write"))))
        } else {
          ShiftFailure("Invalid credentials").toTry
        }
      case _ => ShiftFailure("Invalid authentication scheme").toTry
    }
  }

  def auth = for {
    r <- req
    user <- authenticate(Loc.loc0(r.language)("login.fail").text)
  } yield user
}