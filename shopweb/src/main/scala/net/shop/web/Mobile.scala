package net.shop.web

import net.shop.web.services.ShopServices
import net.shop.web.services.ServiceDependencies
import net.shift.common.TraversingSpec
import net.shift.engine.utils.ShiftUtils
import net.shop.web.services.SecuredService
import net.shift.common.DefaultLog
import net.shift.template.Selectors
import net.shift.io.IODefaults
import net.shop.web.pages.mobile.LandingPage
import net.shift.common.Path
import net.shop.web.pages.mobile.ProductPage
import net.shop.web.pages.mobile.ProductsPage
import net.shop.api.persistence.Persistence
import net.shop.mongodb.MongoDBPersistence
import net.shift.common.Config
import net.shop.web.pages.mobile.CartPage

case class Mobile(cfg: Config, store: Persistence) extends ShopServices { self =>

  val landingPage = new LandingPage {
    val cfg = self.cfg
    val store = self.store
  }

  val productsPage = new ProductsPage {
    val cfg = self.cfg
    val store = self.store
  }

  val productPage = new ProductPage {
    val cfg = self.cfg
    val store = self.store
  }

  val cartPage = new CartPage {
    val cfg = self.cfg
    val store = self.store
  }

  def mobilePages = for {
    p <- mobilePage("mobile", Path("web/mobile/landing.html"), landingPage) |
      mobilePage("mobile/products", Path("web/mobile/products.html"), productsPage) |
      mobilePage("mobile/product", Path("web/mobile/product.html"), productPage) |
      mobilePage("mobile/cart", Path("web/mobile/cart.html"), cartPage)
  } yield {
    p
  }

}
