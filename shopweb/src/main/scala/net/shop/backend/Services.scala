package net.shop
package backend

import scala.util.Try

trait ProductsService {
  def byId(id: String): Try[ProductDetail]
  def allProducts(): Try[List[ProductDetail]]
}

object ProductDetail {
  def apply(id: String, title: String, price: Double, images: List[String]) = 
    new ProductDetail(id, title, None, price, images)
}
case class ProductDetail(id: String, title: String, description: Option[String], price: Double, images: List[String])
