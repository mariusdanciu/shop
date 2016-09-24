package net.shop
package web.services

import scala.util.Failure
import scala.util.Success
import net.shift.common.Base64
import net.shift.common.Config
import net.shift.common.DefaultLog
import net.shift.common.Invalid
import net.shift.common.Path
import net.shift.common.PathObj
import net.shift.common.TraversingSpec
import net.shift.common.Valid
import net.shift.common.Validation
import net.shift.common.Validator
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http._
import net.shift.engine.http.HttpPredicates._
import net.shift.loc.Language
import net.shift.loc.Loc
import net.shift.security.Permission
import net.shift.security.User
import net.shop.api.CompanyInfo
import net.shop.api.ShopError
import net.shop.api.UserDetail
import net.shop.api.UserInfo
import net.shop.model.FieldError
import net.shop.model.Formatters._
import net.shop.web.services.FormImplicits._
import net.shop.web.pages.ForgotPasswordPage
import net.shop.messaging.ForgotPassword
import net.shift.template.PageState
import net.shop.messaging.Messaging
import net.shift.engine.page.Html5
import net.shop.api.Formatter
import net.shift.template.Template._
import net.shift.http.Responses._
import net.shift.http.ContentType._
import net.shift.http.HTTPRequest
import net.shift.http.HTTPParam
import net.shift.io.IO
import net.shift.http.HTTPUtils
import scala.util.Try

trait UserService extends TraversingSpec
    with DefaultLog
    with FormValidation
    with SecuredService
    with ServiceDependencies {

  def deleteAnyUser = for {
    r <- delete
    PathObj(_, _ :: "delete" :: "user" :: email :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    implicit val lang = r.language
    if (user.hasAllPermissions(Permission("write"))) {
      store.deleteUserByEmail(email) match {
        case Success(1) =>
          service(_(ok))
        case scala.util.Failure(err: ShopError) =>
          service(_(serverError.withTextBody(Formatter.format(err))))
        case _ =>
          service(_(notFound.withTextBody(Loc.loc(r.language)("user.not.found", Seq(user.name)).text)))
      }
    } else {
      service(_(forbidden.withTextBody(Formatter.format(new ShopError("no.permissions")))))
    }
  }

  def deleteThisUser = for {
    r <- delete
    _ <- path("/user")
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    store.deleteUserByEmail(user.name) match {
      case Success(1) =>
        service(_(ok.dropSecurityCookies))
      case scala.util.Failure(err: ShopError) =>
        implicit val lang = r.language
        service(_(serverError.withTextBody(Formatter.format(err))))
      case _ =>
        service(_(notFound.withTextBody(Loc.loc(r.language)("user.not.found", Seq(user.name)).text)))
    }
  }

  def userInfo = for {
    r <- get
    Path(_, _ :: "userinfo" :: Nil) <- path
    user <- userRequired(Loc.loc0(r.language)("login.fail").text)
  } yield {
    store.userByEmail(user.name) match {
      case Success(Some(ud)) =>
        implicit val l = r.language
        service(_(ok.withJsonBody(Formatter.format(ud))))
      case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case _ =>
        service(_(notFound.withTextBody(Loc.loc(r.language)("user.not.found", Seq(user.name)).text)))
    }
  }

  def forgotPassword = for {
    r <- post
    Path(_, _ :: "forgotpassword" :: b64 :: Nil) <- path
  } yield {
    val email = Base64.decodeString(b64)
    println(store.userByEmail(email))
    (for {
      Some(ud) <- store.userByEmail(email)
      (_, n) <- Html5.runPageFromFile(PageState(ud, r.language), Path(s"web/templates/forgotpassword_${r.language.name}.html"), ForgotPasswordPage)
    } yield {
      Messaging.send(ForgotPassword(r.language, email, n.toString))
    }) match {
      case Success(_) => service(_(ok.withTextBody(Loc.loc(r.language)("forgotpass.mail.sent", Seq(email)).text)))
      case Failure(t) =>
        service(_(notFound.withTextBody(Loc.loc(r.language)("user.not.found", Seq(email)).text)))
    }
  }

  def createUser = for {
    r <- post
    Path(_, _ :: "create" :: "user" :: Nil) <- path
  } yield {
    implicit val loc = r.language
    extract(r) match {
      case Success(Valid(u)) =>

        val ui = UserInfo(
          firstName = u.firstName,
          lastName = u.lastName,
          cnp = u.cnp,
          phone = u.phone)

        val perms = List("read") ++ (if (cfg.string("admin.user") == u.email) List("write") else Nil)

        val usr = UserDetail(id = None,
          userInfo = ui,
          addresses = Nil,
          email = u.email,
          password = u.password,
          permissions = perms)

        store.createUsers(usr) match {
          case scala.util.Success(ids) =>
            service(_(created.withSecurityCookies(User(usr.email, None, usr.permissions.map { Permission(_) }.toSet))))
          case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
          case scala.util.Failure(t) =>
            error("Cannot create user ", t)
            service(_(serverError.withTextBody(Loc.loc0(r.language)("user.cannot.create").text)))
        }

      case Success(Invalid(msgs)) =>
        validationFail(msgs)
      case Failure(t) =>
        log.error("Cannot create user", t)
        service(_(badRequest))
    }
  }

  private def extract(r: HTTPRequest)(implicit loc: Language): Try[Validation[CreateUser, FieldError]] = {
    val user = (CreateUser.apply _).curried
    val ? = Loc.loc0(loc) _

    val userFormlet = Validator(user) <*>
      Validator(required("cu_firstName", ?("first.name").text, Valid(_))) <*>
      Validator(required("cu_lastName", ?("last.name").text, Valid(_))) <*>
      Validator(optional("cu_cnp", "", Valid(_))) <*>
      Validator(optional("cu_phone", "", Valid(_))) <*>
      Validator(validateCreateUser("cu_email", ?("email").text)) <*>
      Validator(required("cu_password", ?("password").text, Valid(_))) <*>
      Validator(required("cu_password2", ?("retype.password").text, Valid(_)))

    val qs = IO.producerToString(r.body)

    for {
      q <- qs
      p <- HTTPUtils.formURLEncodedToParams(q)
    } yield {
      val params = p map { case HTTPParam(k, v) => (k, v) } toMap

      (userFormlet validate params flatMap {
        case p @ CreateUser(_,
          _,
          _,
          _,
          _,
          pass,
          pass2) if (pass != pass2) =>
          Invalid(List(FieldError("cu_password2", Loc.loc0(loc)("password.not.match").text)))
        case p =>
          Valid(p)
      })
    }

  }

}

case class CreateUser(firstName: String,
                      lastName: String,
                      cnp: String,
                      phone: String,
                      email: String,
                      password: String,
                      password2: String)