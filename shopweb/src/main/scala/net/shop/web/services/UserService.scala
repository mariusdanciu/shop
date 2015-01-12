package net.shop
package web.services

import scala.util.Failure
import scala.util.Success
import net.shift.common.Base64
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.page.Html5
import net.shift.engine.utils.ShiftUtils
import net.shift.html.Formlet
import net.shift.html.Formlet._
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.PageState
import net.shift.template.Selectors
import net.shift.template.SnipState
import net.shop.api.UserDetail
import net.shop.messaging.ForgotPassword
import net.shop.messaging.Messaging
import net.shop.web.ShopApplication
import net.shop.web.pages.ForgotPasswordPage
import net.shop.api.UserInfo

object UserService extends PathUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with ShiftUtils {

  implicit val reqSelector = bySnippetAttr[SnipState[UserDetail]]

  def forgotPassword = for {
    r <- POST
    Path("forgotpassword" :: b64 :: Nil) <- path
  } yield {
    val email = Base64.decodeString(b64)
    (for {
      ud <- ShopApplication.persistence.userByEmail(email)
      (_, n) <- Html5.runPageFromFile(PageState(ud, r.language), Path(s"web/templates/forgotpassword_${r.language.language}.html"), ForgotPasswordPage)
    } yield {
      Messaging.send(ForgotPassword(r.language, email, n.toString))
    }) match {
      case Success(_) => service(_(Resp.ok))
      case Failure(t) =>
        service(_(Resp.notFound.asText.withBody(Loc.loc(r.language)("user.not.found", Seq(email)).text)))
    }

  }

  def createUser = for {
    r <- POST
    Path("create" :: "user" :: Nil) <- path
  } yield {
    implicit val loc = r.language
    extract(r) match {
      case net.shift.html.Success(u) =>

        val ui = UserInfo(
          firstName = u.firstName,
          lastName = u.lastName,
          cnp = u.cnp,
          phone = u.phone.getOrElse(""))

        val usr = UserDetail(id = None,
          userInfo = ui,
          companyInfo = None,
          addresses = Nil,
          email = u.email,
          password = u.password,
          permissions = List("read"))

        ShopApplication.persistence.createUsers(usr) match {
          case scala.util.Success(ids) =>
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create user ", t)
            service(_(Resp.serverError.withBody(Loc.loc0(r.language)("user.cannot.create").text)))
        }

      case net.shift.html.Failure(msgs) => validationFail(msgs)
    }
  }

  private def extract(r: Request)(implicit loc: Language): Validation[ValidationError, CreateUser] = {
    val user = (CreateUser.apply _).curried
    val ? = Loc.loc0(loc) _

    val userFormlet = Formlet(user) <*>
      inputText("cu_firstName")(validateText("cu_firstName", ?("first.name").text)) <*>
      inputText("cu_lastName")(validateText("cu_lastName", ?("last.name").text)) <*>
      inputText("cu_cnp")(validateText("cu_cnp", ?("cnp").text)) <*>
      inputText("cu_phone")(validateOptional("cu_phone", Some(_))) <*>
      inputText("cu_email")(validateCreateUser("cu_email", ?("email").text)) <*>
      inputPassword("cu_password")(validateText("cu_password", ?("password").text)) <*>
      inputPassword("cu_password2")(validateText("cu_password2", ?("retype.password").text))

    (userFormlet validate r.params flatMap {
      case p @ CreateUser(_,
        _,
        _,
        _,
        _,
        pass,
        pass2) if (pass != pass2) =>
        net.shift.html.Failure(List(("cu_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        net.shift.html.Success(p)
    })
  }

}

case class CreateUser(firstName: String,
                      lastName: String,
                      cnp: String,
                      phone: Option[String],
                      email: String,
                      password: String,
                      password2: String)