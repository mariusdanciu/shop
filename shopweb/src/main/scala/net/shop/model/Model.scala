package net.shop
package model

import net.shift.loc.Language
import java.util.Date

case class ProductDetail(id: String,
  title: Map[String, String],
  price: Double,
  oldPrice: Option[Double],
  categories: List[String],
  images: List[String],
  keyWords: List[String]) {

  def title_?(l: Language) = title.getOrElse(l.language, "???")

  def toProductLog(quantity: Int) = ProductLog(id, price, quantity)
}

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: String, val title: Map[String, String], image: String) {
  def title_?(l: Language) = title.getOrElse(l.language, "???")
}

sealed trait Submitter

case class Person(firstName: String, lastName: String) extends Submitter
case class Company(companyName: String, cif: String, regCom: String, bank: String, bankAccount: String) extends Submitter

case class Order(id: String,
  submitter: Submitter,
  region: String,
  city: String,
  address: String,
  email: String,
  phone: String,
  terms: Boolean,
  items: List[(ProductDetail, Int)]) {

  def toOrderLog = OrderLog(id, new Date(), submitter, region, city, address, email, phone, items map { i => i._1.toProductLog(i._2) })
}

case class OrderLog(id: String,
  time: Date,
  submitter: Submitter,
  region: String,
  city: String,
  address: String,
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


