package net.shop.web.pages

import net.shift.common.Xml
import net.shift.server.http.Request
import net.shift.template.Binds.bind
import net.shop.utils.ShopUtils

import scala.util.Success
import scala.xml.{NodeSeq, Text}

/**
  * Created by mariu on 1/29/2017.
  */
trait SiteMapPage extends PageCommon[Request] {
  self =>
  val categories = reqSnip("categories") {
    s =>
      val res = store.allCategories map { cats =>
        for {c <- cats} yield {
          bind(s.node) {
            case Xml("loc", attrs, _) =>
              Xml("loc", attrs, Text(s"http://${cfg.string("host")}/products/${ShopUtils.nameToPath(c)}"))
            case n => n
          } getOrElse s.node
        }
      } getOrElse NodeSeq.Empty flatten

      Success(s.state.initialState -> NodeSeq.fromSeq(res.toSeq))
  }
  val products = reqSnip("products") {
    s =>
      val res = store.allProducts map { prods =>
        for {c <- prods} yield {
          bind(s.node) {
            case Xml("loc", attrs, _) =>
              Xml("loc", attrs, Text(s"http://${cfg.string("host")}/product/${ShopUtils.nameToPath(c)}"))
            case n => n
          } getOrElse s.node
        }
      } getOrElse NodeSeq.Empty flatten

      Success(s.state.initialState -> NodeSeq.fromSeq(res.toSeq))
  }

  override def snippets = List(categories, products) ++ super.snippets


}
