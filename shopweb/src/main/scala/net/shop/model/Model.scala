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

case class Order(id: String,
  firstName: String,
  lastName: String,
  cnp: String,
  region: String,
  city: String,
  address: String,
  email: String,
  phone: String,
  terms: Boolean,
  items: List[(ProductDetail, Int)]) {

  def ownerAsList = List(id, firstName, lastName, cnp, region, city, address, email, phone)
  
}

