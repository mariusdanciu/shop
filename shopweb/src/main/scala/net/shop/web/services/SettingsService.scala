package net.shop
package web.services

import net.shift.common.TraversingSpec
import net.shift.engine.utils.ShiftUtils
import net.shift.common.DefaultLog
import net.shift.template.Selectors
import net.shift.common.PathUtils
import net.shift.engine.http.POST
import net.shift.common.Path
import net.shift.loc.Loc
import net.shift.loc.Language
import net.shift.html.Formlet
import net.shift.html.Formlet._
import net.shift.engine.http.Request
import net.shift.html.Validation
import net.shop.web.ShopApplication
import net.shift.security.User

object SettingsService extends PathUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  def createUser = for {
    r <- POST
    Path("update" :: "settings" :: Nil) <- path
    u <- user
  } yield {
    implicit val loc = r.language
  }

  private def extract(r: Request, u: User)(implicit loc: Language): Validation[ValidationError, CreateUser] = {
    val user = (CreateUser.apply _).curried
    val ? = Loc.loc0(loc) _

    val userFormlet = Formlet(user) <*>
      inputText("update_firstName")(validateText("update_firstName", ?("first.name").text)) <*>
      inputText("update_lastName")(validateText("update_lastName", ?("last.name").text)) <*>
      inputText("update_cnp")(validateText("update_cnp", ?("cnp").text)) <*>
      inputText("update_phone")(validateOptional("update_phone", Some(_))) <*>
      inputText("update_email")(validateDefault("update_email", u.name)) <*>
      inputPassword("update_password")(validateText("update_password", ?("password").text)) <*>
      inputPassword("update_password2")(validateText("update_password2", ?("retype.password").text))

    (userFormlet validate r.params flatMap {
      case p @ CreateUser(_,
        _,
        _,
        _,
        _,
        pass,
        pass2) if (pass != pass2) =>
        net.shift.html.Failure(List(("update_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        net.shift.html.Success(p)
    })
  }

}



