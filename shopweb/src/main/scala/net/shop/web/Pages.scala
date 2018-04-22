package net.shop.web

import net.shift.common.Config
import net.shift.server.http.Request
import net.shop.persistence.Persistence
import net.shop.web.pages._
import net.shop.web.services.ServiceDependencies

/**
  * Created by marius on 1/15/2017.
  */
case class Pages(cfg: Config, store: Persistence) extends ServiceDependencies {
  pages =>

  val catPage = new CategoriesPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val prodDetailPage = new ProductDetailPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val categoryPage = new CategoryPage {
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

  val saveCategoryPage = new SaveCategoryPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val newUserPage = new NewUserPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val siteMapPage = new SiteMapPage {
    val cfg = pages.cfg
    val store = pages.store
  }

  val termsPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }

  val dataProtectionPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }

  val returnPolicyPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }

  val cookiesPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }

  val aboutUsPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }

  val loginPage = new PageCommon[Request] {
    val cfg = pages.cfg
    val store = pages.store
  }
}
