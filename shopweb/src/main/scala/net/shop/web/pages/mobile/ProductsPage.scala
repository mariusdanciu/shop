package net.shop.web.pages.mobile

import net.shop.web.services.ServiceDependencies
import net.shop.web.pages.Cart
import net.shift.engine.http.Request

trait ProductsPage extends Cart[Request] with ServiceDependencies { self =>

  override def snippets = List() ++ super.snippets

}