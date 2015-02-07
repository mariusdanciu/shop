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
import net.shift.html.Formlet._
import net.shift.html.Validation
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.template.PageState
import net.shift.template.Selectors
import net.shift.template.SnipState
import net.shop.api.CompanyInfo
import net.shop.api.Formatter
import net.shop.api.UserDetail
import net.shop.api.UserInfo
import net.shop.messaging.ForgotPassword
import net.shop.messaging.Messaging
import net.shop.model.Formatters._
import net.shop.model.ValidationFail
import net.shop.web.ShopApplication
import net.shop.web.pages.ForgotPasswordPage
import net.shop.web.services.FormImplicits._
import net.shift.html.Formlet
import net.shop.model.FieldError
import net.shift.html.Valid
import net.shift.html.Invalid

object UserService extends PathUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  implicit val reqSelector = bySnippetAttr[SnipState[UserDetail]]

  def userInfo = for {
    r <- GET
    Path("userinfo" :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    ShopApplication.persistence.userByEmail(user.name) match {
      case Success(ud) =>
        implicit val l = r.language.name
        service(_(JsonResponse(Formatter.format(ud))))
      case _ =>
        service(_(Resp.notFound.asText.body(Loc.loc(r.language)("user.not.found", Seq(user.name)).text)))
    }
  }

  def forgotPassword = for {
    r <- POST
    Path("forgotpassword" :: b64 :: Nil) <- path
  } yield {
    val email = Base64.decodeString(b64)
    (for {
      ud <- ShopApplication.persistence.userByEmail(email)
      (_, n) <- Html5.runPageFromFile(PageState(ud, r.language), Path(s"web/templates/forgotpassword_${r.language.name}.html"), ForgotPasswordPage)
    } yield {
      Messaging.send(ForgotPassword(r.language, email, n.toString))
    }) match {
      case Success(_) => service(_(TextResponse(Loc.loc(r.language)("forgotpass.mail.sent", Seq(email)).text)))
      case Failure(t) =>
        service(_(Resp.notFound.asText.body(Loc.loc(r.language)("user.not.found", Seq(email)).text)))
    }
  }

  def createUser = for {
    r <- POST
    Path("create" :: "user" :: Nil) <- path
  } yield {
    implicit val loc = r.language
    extract(r) match {
      case Valid(u) =>

        val ui = UserInfo(
          firstName = u.firstName,
          lastName = u.lastName,
          cnp = u.cnp,
          phone = u.phone)

        val usr = UserDetail(id = None,
          userInfo = ui,
          companyInfo = CompanyInfo("", "", "", "", "", ""),
          addresses = Nil,
          email = u.email,
          password = u.password,
          permissions = List("read"))

        ShopApplication.persistence.createUsers(usr) match {
          case scala.util.Success(ids) =>
            service(_(Resp.created))
          case scala.util.Failure(t) =>
            error("Cannot create user ", t)
            service(_(Resp.serverError.body(Loc.loc0(r.language)("user.cannot.create").text)))
        }

      case Invalid(msgs) => validationFail(msgs)(r.language.name)
    }
  }

  private def extract(r: Request)(implicit loc: Language): Validation[ValidationFail, CreateUser] = {
    val user = (CreateUser.apply _).curried
    val ? = Loc.loc0(loc) _

    val userFormlet = Formlet(user) <*>
      inputText("cu_firstName")(required("cu_firstName", ?("first.name").text, Valid(_))) <*>
      inputText("cu_lastName")(required("cu_lastName", ?("last.name").text, Valid(_))) <*>
      inputText("cu_cnp")(required("cu_cnp", ?("cnp").text, Valid(_))) <*>
      inputText("cu_phone")(required("cu_phone", ?("phone").text, Valid(_))) <*>
      inputText("cu_email")(validateCreateUser("cu_email", ?("email").text)) <*>
      inputPassword("cu_password")(required("cu_password", ?("password").text, Valid(_))) <*>
      inputPassword("cu_password2")(required("cu_password2", ?("retype.password").text, Valid(_)))

    (userFormlet validate r.params flatMap {
      case p @ CreateUser(_,
        _,
        _,
        _,
        _,
        pass,
        pass2) if (pass != pass2) =>
        Invalid(ValidationFail(FieldError("cu_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        Valid(p)
    })
  }

}

case class CreateUser(firstName: String,
                      lastName: String,
                      cnp: String,
                      phone: String,
                      email: String,
                      password: String,
                      password2: String)