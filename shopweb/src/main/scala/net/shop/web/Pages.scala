package net.shop.web

import net.shift.common.Config
import net.shop.api.persistence.Persistence
import net.shop.web.pages._
import net.shop.web.services.ServiceDependencies

/**
  * Created by marius on 1/15/2017.
  */
case class Pages(cfg: Config, store: Persistence) extends ServiceDependencies { pages =>

  val catPage = new CategoryPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val prodDetailPage = new ProductDetailPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val productsPage = new ProductsPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val accPage = new AccountSettingsPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val cartPage = new CartPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val saveProductPage = new SaveProductPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val newUserPage = new NewUserPage {
    val cfg = pages.cfg
    val store = pages.store
  }

}