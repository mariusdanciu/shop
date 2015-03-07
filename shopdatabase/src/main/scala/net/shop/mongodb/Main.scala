package net.shop
package mongodb

import com.mongodb.casbah.Imports._
import java.io.File
import net.shift.common.PathUtils._
import net.shift.common.Path

import net.shop.api._
import net.shop.api.persistence._

object Main extends App {
  val db = MongoClient("localhost")("idid")

  db.dropDatabase

  val ornamente = Category(
    title = Map("ro" -> "Ornamente"),
    position = 0)

  val cats = List(ornamente)

  val ids = MongoDBPersistence.createCategories(cats: _*) map {
    case ids =>
      for (id <- ids) {
        println("Category " + id)
      }
  }
}