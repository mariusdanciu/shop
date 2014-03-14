package net.shop.backend.impl

import net.shop.backend.ProductsService
import net.shop.model.ProductDetail
import scala.util.Try
import net.shop.model.Category
import scala.util.Success

class CachingBackend(serv: ProductsService) extends ProductsService {

  lazy val products = serv.allProducts
  lazy val categories = serv.allCategories

  def productById(id: String): Try[ProductDetail] = filter(p => p.id == id).map { l => l.head }

  def allProducts: Try[Traversable[ProductDetail]] = products

  def categoryProducts(cat: String): Try[Traversable[ProductDetail]] = filter { p =>
    p.categories.contains(cat)
  }

  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]] = allProducts match {
    case Success(prods) => Success(for (p <- prods if f(p)) yield p)
    case f => f
  } 
    
  def categoryById(id: String): Try[Category] = (for {
    p <- categories if (!p.find(c => c.id == id).isEmpty)
  } yield p).map { l => l.head }

  def allCategories = categories

  def searchProducts(text: String): Try[Traversable[ProductDetail]] = serv.searchProducts(text)
}