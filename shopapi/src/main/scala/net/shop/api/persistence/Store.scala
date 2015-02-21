package net.shop.api
package persistence

import scala.util.Try
import scala.util.Failure



trait Persistence {
  def productById(id: String): Try[ProductDetail]
  def allProducts: Try[Iterator[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]]
  
  def categoryById(id: String): Try[Category]
  def allCategories: Try[Iterator[Category]]
  
  def createProducts(prod: ProductDetail*): Try[Seq[String]]
  def updateProducts(prod: ProductDetail*): Try[Seq[String]]
  def deleteProducts(prod: String*): Try[Int]
  
  def createCategories(prod: Category*): Try[Seq[String]]
  def updateCategories(prod: Category*): Try[Seq[String]]
  def deleteCategories(prod: String*): Try[Int]
  
  def createUsers(user: UserDetail*): Try[Seq[String]]
  def updateUsers(user: UserDetail*): Try[Seq[String]]
  def deleteUsers(userId: String*): Try[Int]
  def allUsers: Try[Iterator[UserDetail]]
  def userByEmail(email: String): Try[UserDetail]
  
  def createOrder(order: OrderLog*): Try[Seq[String]]
  def ordersByEmail(email: String): Try[Iterator[OrderLog]]
  def ordersByProduct(productId: String): Try[Iterator[OrderLog]]
  
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

