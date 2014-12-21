package net.shop
package persistence.file

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import net.shop.api.Category
import net.shop.api.ProductDetail
import net.shop.api.persistence.NoSort
import net.shop.api.persistence.Persistence
import net.shop.api.persistence.SortSpec
import net.shift.common.ShiftFailure

case class CachingBackend(serv: Persistence) extends Persistence {

  private var products = for (t <- serv.allProducts) yield {
    ((Nil: List[ProductDetail]) /: t)((a, e) => e :: a)
  }

  private val categories = for (t <- serv.allCategories) yield {
    ((Nil: List[Category]) /: t)((a, e) => e :: a)
  }

  override def productById(id: String): Try[ProductDetail] = {
    filter(p => p.stringId == id).map { l => l.next }
  }

  override def allProducts: Try[Iterator[ProductDetail]] = products map { _ iterator }

  override def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = serv.categoryProducts(cat, spec)

  def filter(f: ProductDetail => Boolean): Try[Iterator[ProductDetail]] = allProducts match {
    case Success(prods) => Success(for (p <- prods if f(p)) yield p)
    case f              => f
  }

  override def categoryById(id: String): Try[Category] = {
    categories match {
      case Success(cats) =>
        cats.find(_.stringId == id) match {
          case Some(c) => Success(c)
          case _       => Failure(new Exception("Category not found"))
        }
      case Failure(f) => Failure(f)
    }
  }
  override def allCategories = categories map (_ iterator)

  override def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = serv.searchProducts(text, spec)

  def createProducts(prod: ProductDetail*): Try[Seq[String]] = serv.createProducts(prod: _*)
  
  def updateProducts(prod: ProductDetail*): Try[Seq[String]] = serv.updateProducts(prod: _*)

  def createCategories(cats: Category*): Try[Seq[String]] = serv.createCategories(cats: _*)

  def deleteProducts(ids: String*): Try[Int] = {
    serv.deleteProducts(ids: _*) match {
      case Success(num) =>
        products = products.map { list => list.filter { p => !ids.contains(p.id) } }
        Success(num)
      case f => f
    }
  }
}