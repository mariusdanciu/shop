package net.shop
package persistence.file

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import net.shop.model.Category
import net.shop.model.ProductDetail
import net.shop.persistence.NoSort
import net.shop.persistence.Persistence
import net.shop.persistence.SortSpec

case class CachingBackend(serv: Persistence) extends Persistence {

  lazy val products = for (t <- serv.allProducts) yield {
    ((Nil: List[ProductDetail]) /: t)((a, e) => e :: a)
  }

  lazy val categories = for (t <- serv.allCategories) yield {
    ((Nil: List[Category]) /: t)((a, e) => e :: a)
  }

  override def productById(id: String): Try[ProductDetail] = filter(p => p.id == id).map { l => l.next }

  override def allProducts: Try[Iterator[ProductDetail]] = products map { _ iterator }

  override def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = serv.categoryProducts(cat, spec)

  def filter(f: ProductDetail => Boolean): Try[Iterator[ProductDetail]] = allProducts match {
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
  override def allCategories = categories map (_ iterator)

  override def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = serv.searchProducts(text, spec)
}