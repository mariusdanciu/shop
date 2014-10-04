package net.shop.mongodb

import com.mongodb.casbah.Imports._

object Test extends App {
  val db = MongoClient("localhost")("idid")

  val products = db("products")
  
  for { p <- db("products") } yield {
    println(p)
  }
}