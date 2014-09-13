package net.shop.mongodb

import com.mongodb.casbah.Imports._

object Main extends App {
  val db = MongoClient("localhost")("idid")

  // db.dropDatabase

  val obj = MongoDBObject(
    "title" -> MongoDBObject(
      "ro" -> "Breitling Chrono Avenger M1"),
    "price" -> 89.99,
    "oldPrice" -> 177.99,
    "categories" -> List("watches", "promotions"),
    "images" -> List("3-1.jpg", "3-2.jpg", "3-3.jpg", "3-4.jpg", "3-5.jpg"),
    "keyWords" -> List("ceasuri"))

  db("products") += obj

  val products = db("products")
  for (p <- products.find()) {
    println(p)
  }
  
  // products.update(MongoDBObject("price" -> 89.99), obj + ("images" -> ("new" :: obj.getAsOrElse[List[String]]("categories", Nil))))
      
}