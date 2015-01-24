package net.shop
package api

import java.util.Date

case class UserInfo(firstName: String, lastName: String, cnp: String, phone: String)
case class CompanyInfo(name: String, cif: String, regCom: String, bank: String, bankAccount: String, phone: String)

case class UserDetail(id: Option[String] = None,
                      userInfo: UserInfo,
                      companyInfo: CompanyInfo,
                      addresses: List[Address],
                      email: String,
                      password: String,
                      permissions: List[String])

case class Address(id: Option[String] = None,
                   name: String,
                   country: String,
                   region: String,
                   city: String,
                   address: String,
                   zipCode: String)

case class ProductDetail(id: Option[String] = None,
                         title: Map[String, String],
                         description: Map[String, String],
                         properties: Map[String, String],
                         price: Double,
                         discountPrice: Option[Double],
                         soldCount: Int,
                         categories: List[String],
                         images: List[String],
                         keyWords: List[String]) {

  def stringId = id getOrElse "?"

  def title_?(l: String) = title.getOrElse(l, "???")

  def toProductLog(quantity: Int) = ProductLog(stringId, price, quantity)
}

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: Option[String] = None, val title: Map[String, String], image: Option[String]) {
  def title_?(l: String) = title.getOrElse(l, "???")
  def stringId = id getOrElse "?"
}

sealed trait Submitter

case class Person(firstName: String, lastName: String, cnp: String) extends Submitter
case class Company(companyName: String, cif: String, regCom: String, bank: String, bankAccount: String) extends Submitter

case class Order(id: String,
                 submitter: Submitter,
                 address: Address,
                 email: String,
                 phone: String,
                 terms: Boolean,
                 items: List[(ProductDetail, Int)]) {

  def toOrderLog = OrderLog(id, new Date(), submitter, address, email, phone, items map { i => i._1.toProductLog(i._2) })
}

case class OrderLog(id: String,
                    time: Date,
                    submitter: Submitter,
                    address: Address,
                    email: String,
                    phone: String,
                    items: List[ProductLog]) {

  lazy val total = (0.0 /: items)((a, i) => a + i.price * i.quantity)
}

case class ProductLog(id: String, price: Double, quantity: Int)

object Formatter {
  def format[T: Formatter](v: T): String = {
    implicitly[Formatter[T]].write(v)
  }
}

trait Formatter[T] {
  def write(value: T): String
}


