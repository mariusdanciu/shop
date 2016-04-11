package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies

trait CartPage extends MobilePage with ServiceDependencies { self =>

  override def snippets = List(cart) ++ super.snippets

  val cart = reqSnip("cart") {
    s =>
      {
        ???
      }
  }

}