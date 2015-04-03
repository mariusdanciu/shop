package net.shop
package web.pages

import scala.xml._
import scala.xml._
import net.shift._
import net.shift._
import net.shift.engine.http._
import net.shift.engine.http._
import net.shift.loc._
import net.shift.template._
import net.shift.template._
import net.shift.template.Binds._
import net.shift.template.Snippet._
import net.shift.template.Snippet._
import net.shop.web.ShopApplication
import scala.util.Success
import scala.util.Failure
import net.shop.utils.ShopUtils

object TermsPage extends Cart[Request] { self =>

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("terms.link").text

}

object DataProtectionPage extends Cart[Request] { self =>

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("data.protection.link").text

}

object ReturnPolicyPage extends Cart[Request] { self =>

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("return.policy.link").text

}

object CookiesPage extends Cart[Request] { self =>

  def pageTitle(s: PageState[Request]) = Loc.loc0(s.lang)("data.cookies.link").text

}