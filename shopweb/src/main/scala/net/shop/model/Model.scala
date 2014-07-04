package net.shop
package model

import net.shift.loc.Language

case class ProductDetail(id: String,
  title: Map[String, String],
  price: Double,
  oldPrice: Option[Double],
  categories: List[String],
  images: List[String],
  keyWords: List[String]) {

  def title_?(l: Language) = title.getOrElse(l.language, "???")
}

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: String, val title: Map[String, String], image: String) {
  def title_?(l: Language) = title.getOrElse(l.language, "???")
}

sealed trait Submiter

case class Person(firstName: String, lastName: String) extends Submiter
case class Company(companyName: String, cif: String, regCom: String, bank: String, bankAccount: String) extends Submiter

case class Order(id: String,
  submiter: Submiter,
  region: String,
  city: String,
  address: String,
  email: String,
  phone: String,
  terms: Boolean,
  items: List[(ProductDetail, Int)]) {

}

