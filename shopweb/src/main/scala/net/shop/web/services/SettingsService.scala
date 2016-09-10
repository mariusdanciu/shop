package net.shop
package web.services

import net.shift.engine.ShiftApplication.service
import net.shift.common.TraversingSpec
import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.loc.Loc
import net.shift.loc.Language
import net.shop.web.ShopApplication
import net.shift.security.User
import net.shop.api.UserInfo
import net.shop.api.CompanyInfo
import net.shop.api.Address
import net.shop.model.ValidationFail
import net.shop.model.FieldError
import net.shop.web.services.FormImplicits._
import net.shop.api.OrderStatus
import scala.util.Success
import scala.util.Failure
import net.shift.security.Permission
import net.shop.api.ShopError
import net.shift.common.Config
import net.shift.common.Valid
import net.shift.common.Validation
import net.shift.common.Invalid
import net.shift.common.Validator
import net.shift.engine.http.HttpPredicates._
import net.shift.http.Responses._
import net.shift.http.ContentType._
import net.shift.http.HTTPRequest
import net.shift.http.HTTPParam

trait SettingsService extends TraversingSpec
    with DefaultLog
    with FormValidation
    with SecuredService
    with ServiceDependencies {

  def updateOrderStatus = for {
    r <- post
    Path(_, _ :: "order" :: "updatestatus" :: orderId :: status :: Nil) <- path
    u <- permissions(Loc.loc0(r.language)("user.not.found").text, Permission("write"))
  } yield {
    store.updateOrderStatus(orderId, OrderStatus.fromIndex(status.toInt)) match {
      case Success(_)                            => service(_(ok))
      case scala.util.Failure(ShopError(msg, _)) => service(_(ok.withTextBody(Loc.loc0(r.language)(msg).text)))
      case Failure(msg)                          => service(_(ok.withTextBody(Loc.loc(r.language)("order.not.found", List(orderId)).text)))
    }
  }

  def updateSettings = for {
    r <- post
    Path(_, _ :: "update" :: "settings" :: Nil) <- path
    u <- user
  } yield {
    u match {
      case Some(usr) =>
        implicit val loc = r.language
        extract(r, usr) match {
          case Valid(u) =>
            store.userByEmail(usr.name) match {
              case scala.util.Success(Some(ud)) =>
                val merged = ud.copy(userInfo = u.user.userInfo,
                  companyInfo = u.user.companyInfo,
                  addresses = u.addresses,
                  password = if (u.user.pass.isEmpty) ud.password else u.user.pass)
                store.updateUsers(merged)

              case _ => service(_(serverError.withTextBody(Loc.loc0(r.language)("login.fail").text)))
            }
            service(_(created.withTextBody(Loc.loc0(r.language)("settings.saved").text)))
          case Invalid(e) =>
            validationFail(e)
        }
      case None => service(_(forbidden.withTextBody(Loc.loc0(r.language)("login.fail").text)))
    }
  }

  private def extractAddresses(params: Map[String, List[String]])(implicit loc: Language): Validation[List[Address], FieldError] = {

    def toPair(s: String): Option[(String, String)] = s.split(":").toList match {
      case name :: value :: Nil => Some((name, value))
      case _                    => None
    }

    def normalize = {
      params.filter { case (k, v) => k contains ":" }.groupBy {
        case (k, v) => toPair(k).map(_._2) getOrElse ""
      }
    }

    val addresses = validateAddresses(normalize)

    val errors = (((List[FieldError]()), Nil: List[Address]) /: addresses)((acc, e) => e match {
      case Invalid(e)  => (acc._1 ::: e, acc._2)
      case Valid(addr) => (acc._1, acc._2 ::: List(addr))
    })

    errors match {
      case (Nil, list) => Valid(list)
      case (errors, _) => Invalid(errors)
    }

  }

  private def validateAddresses(res: Map[String, Map[String, List[String]]])(implicit loc: Language) = {
    val ? = Loc.loc0(loc) _
    val address = (Address.apply _).curried(None)
    for {
      (k, par) <- res
    } yield {
      val addrFormlet = Validator(address(k)("Romania")) <*>
        Validator(required(s"addr_region:$k", ?("region").text, Valid(_))) <*>
        Validator(required(s"addr_city:$k", ?("city").text, Valid(_))) <*>
        Validator(required(s"addr_addr:$k", ?("address").text, Valid(_))) <*>
        Validator(required(s"addr_zip:$k", ?("zip").text, Valid(_)))
      addrFormlet validate par
    }
  }

  private def extract(r: HTTPRequest, u: User)(implicit loc: Language): Validation[UserForm, FieldError] = {
    val ? = Loc.loc0(loc) _

    val ui = (UserInfo.apply _).curried
    val ci = (CompanyInfo.apply _).curried
    val uu = (UpdateUser.apply _).curried

    val uiFormlet = Validator(ui) <*>
      Validator(validateText("update_firstName", ?("first.name").text)) <*>
      Validator(validateText("update_lastName", ?("last.name").text)) <*>
      Validator(validateText("update_cnp", ?("cnp").text)) <*>
      Validator(validateText("update_phone", ?("phone").text))

    val ciFormlet = Validator(ci) <*>
      Validator(validateText("update_cname", ?("company.name").text)) <*>
      Validator(validateText("update_cif", ?("compnay.cif").text)) <*>
      Validator(validateText("update_cregcom", ?("company.reg.com").text)) <*>
      Validator(validateText("update_cbank", ?("company.bank").text)) <*>
      Validator(validateText("update_cbankaccount", ?("company.bank.account").text)) <*>
      Validator(validateText("update_cphone", ?("phone").text))

    val updateFormlet = Validator(uu) <*> uiFormlet <*> ciFormlet <*>
      Validator(validateText("update_password", ?("password").text)) <*>
      Validator(validateText("update_password2", ?("retype.password").text))

    val params = r.uri.params.map { case HTTPParam(name, values) => (name, values) }.toMap

    val valid = (updateFormlet validate params flatMap {
      case p @ UpdateUser(_,
        _,
        pass,
        pass2) if (pass != pass2) =>
        Invalid(List(FieldError("update_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        Valid(p)
    })

    (valid, extractAddresses(params)) match {
      case (Invalid(l), Invalid(r)) => Invalid(l ++ r)
      case (Invalid(l), _)          => Invalid(l)
      case (_, Invalid(r))          => Invalid(r)
      case (Valid(l), Valid(r))     => Valid(UserForm(l, r))
    }

  }
}

case class UpdateUser(userInfo: UserInfo, companyInfo: CompanyInfo, pass: String, verifyPass: String)
case class UserForm(user: UpdateUser, addresses: List[Address])


