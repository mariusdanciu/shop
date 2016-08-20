package net.shop.web.services.mobile

import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.string2JsonInput
import net.shift.common.DefaultLog
import net.shift.common.TraversingSpec
import net.shift.engine.http.GET
import net.shift.engine.page.Html5
import net.shift.io.FileSystem
import net.shift.io.IODefaults
import net.shop.web.pages.mobile.CartPage
import net.shop.web.services.FormValidation
import net.shop.web.services.SecuredService
import net.shop.web.services.ServiceDependencies
import net.shift.template.PageState
import net.shift.common.Path
import net.shift.engine.ShiftApplication.service
import net.shift.engine.http.Html5Response
import net.shift.loc.Loc
import net.shift.engine.http.Request

import net.shift.engine.http.HttpPredicates._
import net.shift.template.Template._

trait MobileServices extends TraversingSpec
    with DefaultLog
    with FormValidation
    with SecuredService
    with ServiceDependencies { self =>

  def cartView(implicit fs: FileSystem) = for {
    r <- GET
    _ <- path("mobile/cartview")
    u <- user
  } yield {
    Html5.pageFromFile(PageState(r, r.language, u),
      Path(s"web/templates/mobile/cartitems.html"), new CartPage {
        val cfg = self.cfg
        val store = self.store
      })
  }
}