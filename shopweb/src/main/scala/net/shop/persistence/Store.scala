package net.shop
package persistence

import scala.util.Try

import net.shift.loc.Language
import net.shop.model.Category
import net.shop.model.ProductDetail

trait Persistence {
  def productById(id: String): Try[ProductDetail]
  def allProducts: Try[Iterator[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  def categoryById(id: String): Try[Category]
  def allCategories: Try[Iterator[Category]]
  def createProducts(prod: ProductDetail*): Try[Seq[String]]
  def createCategories(prod: Category*): Try[Seq[String]]
}

object SortSpec {
  def fromString(v: String, lang: Language): SortSpec =
    if (v == null) NoSort
    else if (v == "bynameasc") SortByName(true, lang)
    else if (v == "bynamedesc") SortByName(false, lang)
    else if (v == "bypriceasc") SortByPrice(true, lang)
    else if (v == "bypricedesc") SortByPrice(false, lang)
    else NoSort
}
sealed trait SortSpec
case object NoSort extends SortSpec
case class SortByName(direction: Boolean, lang: Language) extends SortSpec
case class SortByPrice(direction: Boolean, lang: Language) extends SortSpec

