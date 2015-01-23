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
import net.shift.html.Failure
import net.shift.html.Success
import net.shift.engine.http.Resp

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
    u <- authenticate(Loc.loc0(r.language)("login.fail").text)
  } yield {
    implicit val loc = r.language
    extract(r, u) match {
      case Success(u) =>
        println(u)
        service(_(Resp.created))
      case Failure(e) => validationFail(e)
    }
  }

  private def extractAddresses(params: Map[String, List[String]])(implicit loc: Language): Validation[ValidationError, List[Address]] = {

    def toPair(s: String): Option[(String, String)] = s.split(":").toList match {
      case name :: value :: Nil => Some((name, value))
      case _                    => None
    }

    def normalize = {
      val p = params.filter { case (k, v) => k contains ":" }.groupBy {
        case (k, v) => toPair(k).map(_._2) getOrElse ""
      }
      p.map {
        case (k, v) =>
          (k, v.map {
            case (vk, vv) => toPair(vk).map(t => (t._1, vv)) getOrElse ("", vv)
          })
      }
    }

    val addresses = validateAddresses(normalize)

    val errors = (((Nil: ValidationError), Nil: List[Address]) /: addresses)((acc, e) => e match {
      case Failure(e)    => (acc._1 ::: e, acc._2)
      case Success(addr) => (acc._1, acc._2 ::: List(addr))
    })

    errors match {
      case (Nil, list) => Success(list)
      case (errors, _) => Failure(errors)
    }

  }

  private def validateAddresses(res: Map[String, Map[String, List[String]]])(implicit loc: Language) = {
    val ? = Loc.loc0(loc) _
    val address = (Address.apply _).curried(None)
    for {
      (k, par) <- res
    } yield {
      val addrFormlet = Formlet(address) <*>
        inputText(s"addr_country:$k")(validateDefault("Romania")) <*>
        inputText(s"addr_region:$k")(validateText(s"addr_region:$k", ?("region").text)) <*>
        inputText(s"addr_city:$k")(validateText(s"addr_city:$k", ?("city").text)) <*>
        inputText(s"addr_addr:$k")(validateText(s"addr_addr:$k", ?("address").text)) <*>
        inputText(s"addr_zip:$k")(validateText(s"addr_zip:$k", ?("zip").text))
      addrFormlet validate par
    }
  }

  private def extract(r: Request, u: User)(implicit loc: Language): Validation[ValidationError, UserForm] = {
    val ? = Loc.loc0(loc) _

    val ui = (UserInfo.apply _).curried
    val ci = (CompanyInfo.apply _).curried
    val uu = (UpdateUser.apply _).curried

    val phoneFunc = (t: String) => t.trim match {
      case "" => None
      case t  => Some(t)
    }

    val uiFormlet = Formlet(ui) <*>
      inputText("update_firstName")(validateText("update_firstName", ?("first.name").text)) <*>
      inputText("update_lastName")(validateText("update_lastName", ?("last.name").text)) <*>
      inputText("update_cnp")(validateText("update_cnp", ?("cnp").text)) <*>
      inputText("update_phone")(validateOptional[String]("update_phone", phoneFunc))

    val ciFormlet = Formlet(ci) <*>
      inputText("update_cname")(validateText("update_cname", ?("company.name").text)) <*>
      inputText("update_cif")(validateText("update_cif", ?("compnay.cif").text)) <*>
      inputText("update_regcom")(validateText("update_regcom", ?("company.reg.com").text)) <*>
      inputText("update_cbank")(validateText("update_cbank", ?("company.bank").text)) <*>
      inputText("update_cbankaccount")(validateText("update_cbankaccount", ?("company.bank.account").text)) <*>
      inputText("update_cphone")(validateOptional("update_cphone", phoneFunc))

    val updateFormlet = Formlet(uu) <*> uiFormlet <*> ciFormlet <*>
      inputText("update_password")(validateText("update_password", ?("password").text)) <*>
      inputText("update_password2")(validateText("update_password2", ?("retype.password").text))

    val valid = (updateFormlet validate r.params flatMap {
      case p @ UpdateUser(_,
        _,
        pass,
        pass2) if (pass != pass2) =>
        net.shift.html.Failure(List(("update_password2", Loc.loc0(loc)("password.not.match").text)))
      case p =>
        net.shift.html.Success(p)
    })

    (valid, extractAddresses(r.params)) match {
      case (Failure(l), Failure(r)) => Failure(l ::: r)
      case (Failure(l), _)          => Failure(l)
      case (_, Failure(r))          => Failure(r)
      case (Success(l), Success(r)) => Success(UserForm(l, r))
    }

  }
}

case class UpdateUser(userInfo: UserInfo, companyInfo: CompanyInfo, pass: String, verifyPass: String)
case class UserForm(user: UpdateUser, addresses: List[Address])


