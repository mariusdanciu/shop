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

  val ps = List(

    ProductDetail(
      title = Map("ro" -> "Diam nonummy"),
      price = 137.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("1.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Chrono Avenger M12"),
      price = 137.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("2.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chrono Avenger M1"),
      price = 89.99,
      oldPrice = Some(177.4),
      soldCount = 0,
      categories = List("watches", "promotions"),
      images = List("3-1.jpg", "3-2.jpg", "3-3.jpg", "3-4.jpg", "3-5.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chronomat Evolution Gold"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("4.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(title = Map("ro" -> "Breitling Chronomat Evolution Gold 2"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("5.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chronomat Evolution Gold 3"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("6.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chronomat Evolution Gold 4"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("7.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chronomat Evolution Gold 5"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("8.jpg"),
      keyWords = List("ceasuri")),

    ProductDetail(
      title = Map("ro" -> "Breitling Chronomat Evolution Gold 6"),
      price = 127.99,
      oldPrice = None,
      soldCount = 0,
      categories = List("watches"),
      images = List("9.jpg"),
      keyWords = List("ceasuri")))

  MongoDBPersistence.createProducts(ps: _*) map { prods =>
    for (id <- prods zipWithIndex) {
      println(id)
      //new File(s"../shopweb/data/products/${id._1}").mkdirs()
      //new File(s"../shopweb/data/products/${id._1}/large").mkdirs()
      //new File(s"../shopweb/data/products/${id._1}/thumb").mkdirs()
      //new File(s"../shopweb/data/products/${id._1}/normal").mkdirs()

      scalax.file.Path.fromString(s"../shopweb/data/products/${id._2 + 1}").copyTo(
        scalax.file.Path.fromString(s"../shopweb/data/products/${id._1}"), true, true, true)
      // println(writeToPath(Path(s"../shopweb/data/products/${id._1}/desc_ro.html"), "<span></span>".getBytes()))
    }
  }

  val cats = List(
    Category(
      title = Map("ro" -> "Ceasuri de marca"),
      image = "ramafoto.png"),

    Category(
      title = Map("ro" -> "Promotii"),
      image = "pernamelc.png"),

    Category(
      title = Map("ro" -> "Ceasuri de marca"),
      image = "juscarii.png"))

  MongoDBPersistence.createCategories(cats: _*) map {
    for (id <- _) {
      println("Category " + id)
    }

  }
}