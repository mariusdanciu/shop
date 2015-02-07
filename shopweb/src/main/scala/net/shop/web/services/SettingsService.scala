package net.shop
package web.services

import net.shift.engine.ShiftApplication.service
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
import net.shop.api.UserInfo
import net.shop.api.CompanyInfo
import net.shop.api.Address
import net.shift.engine.http.Resp
import net.shop.model.ValidationFail
import net.shop.model.FieldError
import net.shop.web.services.FormImplicits._
import net.shift.html.Valid
import net.shift.html.Invalid

object SettingsService extends PathUtils
  with Selectors
  with TraversingSpec
  with DefaultLog
  with FormValidation
  with SecuredService {

  val k = updateSettings

  def updateSettings = for {
    r <- POST
    Path("update" :: "settings" :: Nil) <- path
    u <- user
  } yield {
    u match {
      case Some(usr) =>
        implicit val loc = r.language
        extract(r, usr) match {
          case Valid(u) =>
            ShopApplication.persistence.userByEmail(usr.name) match {
              case scala.util.Success(ud) =>
                val merged = ud.copy(userInfo = u.user.userInfo,
                  companyInfo = u.user.companyInfo,
                  addresses = u.addresses,
                  password = if (u.user.pass.isEmpty) ud.password else u.user.pass)
                ShopApplication.persistence.updateUsers(merged)

              case _ => service(_(Resp.serverError.asText.body(Loc.loc0(r.language)("login.fail").text)))
            }
            service(_(Resp.created.asText.body(Loc.loc0(r.language)("settings.saved").text)))
          case Invalid(e) => validationFail(e)(r.language.name)
        }
      case None => service(_(Resp.forbidden.asText.body(Loc.loc0(r.language)("login.fail").text)))
    }
  }

  private def extractAddresses(params: Map[String, List[String]])(implicit loc: Language): Validation[ValidationFail, List[Address]] = {

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

    val errors = (((ValidationFail()), Nil: List[Address]) /: addresses)((acc, e) => e match {
      case Invalid(e)    => (acc._1 append e, acc._2)
      case Valid(addr) => (acc._1, acc._2 ::: List(addr))
    })

    errors match {
      case (ValidationFail(Nil), list) => Valid(list)
      case (errors, _)                 => Invalid(errors)
    }

  }

  private def validateAddresses(res: Map[String, Map[String, List[String]]])(implicit loc: Language) = {
    val ? = Loc.loc0(loc) _
    val address = (Address.apply _).curried(None)
    for {
      (k, par) <- res
    } yield {
      val addrFormlet = Formlet(address(k)("Romania")) <*>
        inputText(s"addr_region:$k")(required(s"addr_region:$k", ?("region").text, Valid(_))) <*>
        inputText(s"addr_city:$k")(required(s"addr_city:$k", ?("city").text, Valid(_))) <*>
        inputText(s"addr_addr:$k")(required(s"addr_addr:$k", ?("address").text, Valid(_))) <*>
        inputText(s"addr_zip:$k")(required(s"addr_zip:$k", ?("zip").text, Valid(_)))
      addrFormlet validate par
    }
  }

  private def extract(r: Request, u: User)(implicit loc: Language): Validation[ValidationFail, UserForm] = {
    val ? = Loc.loc0(loc) _

    val ui = (UserInfo.apply _).curried
    val ci = (CompanyInfo.apply _).curried
    val uu = (UpdateUser.apply _).curried

    val uiFormlet = Formlet(ui) <*>
      inputText("update_firstName")(validateText("update_firstName", ?("first.name").text)) <*>
      inputText("update_lastName")(validateText("update_lastName", ?("last.name").text)) <*>
      inputText("update_cnp")(validateText("update_cnp", ?("cnp").text)) <*>
      inputText("update_phone")(validateText("update_phone", ?("phone").text))

    val ciFormlet = Formlet(ci) <*>
      inputText("update_cname")(validateText("update_cname", ?("company.name").text)) <*>
      inputText("update_cif")(validateText("update_cif", ?("compnay.cif").text)) <*>
      inputText("update_cregcom")(validateText("update_cregcom", ?("company.reg.com").text)) <*>
      inputText("update_cbank")(validateText("update_cbank", ?("company.bank").text)) <*>
      inputText("update_cbankaccount")(validateText("update_cbankaccount", ?("company.bank.account").text)) <*>
      inputText("update_cphone")(validateText("update_cphone", ?("phone").text))

    val updateFormlet = Formlet(uu) <*> uiFormlet <*> ciFormlet <*>
      inputText("update_password")(validateText("update_password", ?("password").text)) <*>
      inputText("update_password2")(validateText("update_password2", ?("retype.password").text))

    val valid = (updateFormlet validate r.params flatMap {
      case p @ UpdateUser(_,
        _,
        pass,
        pass2) if (pass != pass2) =>
        Invalid(ValidationFail(FieldError("update_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        Valid(p)
    })

    (valid, extractAddresses(r.params)) match {
      case (Invalid(l), Invalid(r)) => Invalid(l append r)
      case (Invalid(l), _)          => Invalid(l)
      case (_, Invalid(r))          => Invalid(r)
      case (Valid(l), Valid(r)) => Valid(UserForm(l, r))
    }

  }
}

case class UpdateUser(userInfo: UserInfo, companyInfo: CompanyInfo, pass: String, verifyPass: String)
case class UserForm(user: UpdateUser, addresses: List[Address])


