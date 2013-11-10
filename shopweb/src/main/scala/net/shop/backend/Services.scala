package net.shop
package backend

import scala.util.Try

trait ProductsService {
  def byId(id: String): Try[ProductDetail]
  def allProducts(): Try[Traversable[ProductDetail]]
  def filter(f: ProductDetail => Boolean) : Try[Traversable[ProductDetail]]
}

case class ProductDetail(id: String, title: String, price: Double, images: List[String])

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])