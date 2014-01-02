package net.shop
package backend

import scala.util.Try

trait ProductsService {
  def productById(id: String): Try[ProductDetail]
  def allProducts(): Try[Traversable[ProductDetail]]
  def categoryProducts(cat: String): Try[Traversable[ProductDetail]]
  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]]

  def categoryById(id: String): Try[Category]
  def allCategories(): Try[Traversable[Category]]
}

case class ProductDetail(id: String, title: String, price: Double, categories: List[String], images: List[String])

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: String, title: String, image: String)

