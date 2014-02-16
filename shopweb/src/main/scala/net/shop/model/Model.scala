package net.shop.model

case class ProductDetail(id: String, title: String, price: Double, categories: List[String], images: List[String])

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: String, title: String, image: String)

case class Order(id: String,
  firstName: String,
  lastName: String,
  region: String,
  city: String,
  address: String,
  email: String,
  phone: String,
  items: List[(String, Int)])

