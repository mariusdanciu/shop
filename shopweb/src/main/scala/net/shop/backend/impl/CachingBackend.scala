package net.shop.backend.impl

import net.shop.backend.ProductsService
import net.shop.model.ProductDetail
import scala.util.Try
import net.shop.model.Category
import scala.util.Success
import scala.util.Failure
import net.shop.backend.NoSort
import net.shop.backend.SortSpec

case class CachingBackend(serv: ProductsService) extends ProductsService {

  lazy val products = serv.allProducts
  lazy val categories = serv.allCategories

  override def productById(id: String): Try[ProductDetail] = filter(p => p.id == id).map { l => l.head }

  override def allProducts: Try[Traversable[ProductDetail]] = products

  override def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]] = serv.categoryProducts(cat, spec)

  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]] = allProducts match {
    case Success(prods) => Success(for (p <- prods if f(p)) yield p)
    case f => f
  }

  override def categoryById(id: String): Try[Category] = {
    categories match {
      case Success(cats) =>
        cats.find(_.id == id) match {
          case Some(c) => Success(c)
          case _ => Failure(new Exception("Category not found"))
        }
      case Failure(f) => Failure(f)
    }
  }
  override def allCategories = categories

  override def searchProducts(text: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]] = serv.searchProducts(text, spec)
}