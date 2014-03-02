package net.shop
package backend

import scala.util.Try
import net.shop.model.ProductDetail
import net.shop.model.Category

trait ProductsService {
  def productById(id: String): Try[ProductDetail]
  def allProducts: Try[Traversable[ProductDetail]]
  def categoryProducts(cat: String): Try[Traversable[ProductDetail]]
  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]]
  def categoryById(id: String): Try[Category]
  def allCategories: Try[Traversable[Category]]
  def searchProducts(text: String): Try[Traversable[ProductDetail]]
}

