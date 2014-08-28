package net.shop
package backend

import scala.util.Try
import net.shop.model.ProductDetail
import net.shop.model.Category
import net.shift.loc.Language

trait ProductsService {
  def productById(id: String): Try[ProductDetail]
  def allProducts: Try[Traversable[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]]

  def categoryById(id: String): Try[Category]
  def allCategories: Try[Traversable[Category]]
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

