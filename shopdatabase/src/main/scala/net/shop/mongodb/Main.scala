package net.shop
package mongodb

import com.mongodb.casbah.Imports._
import java.io.File
import net.shift.common.PathUtils
import net.shift.common.Path

import net.shop.api._
import net.shop.api.persistence._

object Main extends App with PathUtils {
  val db = MongoClient("localhost")("idid")

  db.dropDatabase

  val ornamente = Category(
    title = Map("ro" -> "Ornamente"),
    image = "jucarii.png")

  val cats = List(ornamente)

  val ids = MongoDBPersistence.createCategories(cats: _*) map {
    case ids =>
      for (id <- ids) {
        println("Category " + id)
      }
  }
}