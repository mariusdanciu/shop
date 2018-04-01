package net.shop.api
package persistence

import scala.concurrent.Future
import scala.util.Try

trait Persistence {

  def makeID: String
  def productById(id: String): Try[ProductDetail]

  def productByName(name: String): Try[ProductDetail]
  def allProducts: Try[Seq[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Seq[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Seq[ProductDetail]]

  def categoryByName(name: String): Try[Category]
  def categoryById(id: String): Try[Category]
  def allCategories: Try[Seq[Category]]

  def createProduct(prod: ProductDetail): Try[String]
  def updateProduct(prod: ProductDetail): Try[String]
  def deleteProduct(id: String): Try[String]

  def createCategory(prod: Category): Try[String]
  def updateCategory(prod: Category): Try[String]
  def deleteCategory(prod: String): Try[String]

  def createOrder(order: OrderLog): Try[String]
  def ordersById(id: String): Try[Seq[OrderLog]]
  def ordersByStatus(status: OrderStatus): Try[Seq[OrderLog]]
  def ordersByProduct(productId: String): Try[Seq[OrderLog]]
  def updateOrderStatus(orderId: String, status: OrderStatus): Try[String]

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

