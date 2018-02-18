package net.shop.api
package persistence

import scala.concurrent.Future

trait Persistence {
  def productById(id: String): Future[ProductDetail]

  def productByName(name: String): Future[ProductDetail]
  def allProducts: Future[Iterator[ProductDetail]]
  def categoryProducts(cat: String, spec: SortSpec = NoSort): Future[Seq[ProductDetail]]
  def searchProducts(text: String, spec: SortSpec = NoSort): Future[Seq[ProductDetail]]

  def categoryByName(name: String): Future[Category]
  def categoryById(id: String): Future[Category]
  def allCategories: Future[Seq[Category]]

  def createProduct(prod: ProductDetail): Future[Seq[String]]
  def updateProduct(prod: ProductDetail): Future[Seq[String]]
  def deleteProduct(id: String): Future[Boolean]

  def createCategory(prod: Category): Future[Seq[String]]
  def updateCategory(prod: Category): Future[Seq[String]]
  def deleteCategory(prod: String): Future[Boolean]

  def createUsers(user: UserDetail*): Future[Seq[String]]
  def updateUsers(user: UserDetail*): Future[Seq[String]]
  def deleteUserByEmail(email: String): Future[Int]
  def deleteUsers(userId: String*): Future[Int]
  def allUsers: Future[Iterator[UserDetail]]
  def userByEmail(email: String): Future[Option[UserDetail]]

  def createOrder(order: OrderLog): Future[String]
  def ordersByEmail(email: String): Future[Iterator[OrderLog]]
  def ordersByStatus(status: OrderStatus): Future[Iterator[OrderLog]]
  def ordersByProduct(productId: String): Future[Iterator[OrderLog]]
  def updateOrderStatus(orderId: String, status: OrderStatus): Future[Boolean]

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

