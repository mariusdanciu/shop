package net.shop.api
package persistence

import scala.util.Try
import scala.util.Failure

object ShopError {
  def fail(msg: String) = Failure(new ShopError(msg))
  def fail(e: Exception) = Failure(new ShopError(e))
}

case class ShopError(msg: String, e: Exception) extends RuntimeException(msg, e) {
  def this(msg: String) = this(msg, null)
  def this(e: Exception) = this(e.getMessage(), e)
}

trait Persistence {
  def productById(id: String): Try[ProductDetail]
  def allProducts: Try[Iterator[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  def categoryById(id: String): Try[Category]
  def allCategories: Try[Iterator[Category]]
  def createProducts(prod: ProductDetail*): Try[Seq[String]]
  def deleteProducts(prod: String*): Try[Int]
  def createCategories(prod: Category*): Try[Seq[String]]
}

object SortSpec {
  def fromString(v: String, lang: String): SortSpec =
    if (v == null) NoSort
    else if (v == "bynameasc") SortByName(true, lang)
    else if (v == "bynamedesc") SortByName(false, lang)
    else if (v == "bypriceasc") SortByPrice(true, lang)
    else if (v == "bypricedesc") SortByPrice(false, lang)
    else NoSort
}
sealed trait SortSpec
case object NoSort extends SortSpec
case class SortByName(direction: Boolean, lang: String) extends SortSpec
case class SortByPrice(direction: Boolean, lang: String) extends SortSpec

