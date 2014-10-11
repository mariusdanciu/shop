package net.shop
package mongodb

import scala.util.Success
import scala.util.Try

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

import net.shop.api._
import net.shop.api.persistence._
import ShopError._

object MongoDBPersistence extends Persistence {

  lazy val db = MongoClient("localhost")("idid")

  db.command(MongoDBObject("setParameter" -> 1, "textSearchEnabled" -> 1))

  db("products").ensureIndex(MongoDBObject("title.ro" -> 1))
  db("products").ensureIndex(MongoDBObject("keywords" -> 1))
  db("products").ensureIndex(MongoDBObject("categories" -> 1))

  def productById(id: String): Try[ProductDetail] = try {
    db("products").findOne(MongoDBObject("_id.$oid" -> id)) match {
      case Some(obj) => Success(mongoToProduct(obj))
      case _ => fail("Item " + id + " not found")
    }
  } catch {
    case e: Exception => fail(e)
  }

  def allProducts: Try[Iterator[ProductDetail]] = try {
    Success(for { p <- db("products").find() } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => fail(e)
  }

  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = try {

    val query = spec match {
      case SortByName(true, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> 1))
      case SortByName(false, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> -1))
      case SortByPrice(true, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> 1))
      case SortByPrice(false, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> -1))
      case _ => db("products").find("categories" $in List(cat))
    }

    Success(for { p <- query } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => fail(e)
  }

  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = try {
    Success(
      for {
        p <- db("products").find(
          $or(
            $or(MongoDBObject("title.ro" -> s".*${text}.*".r),
              MongoDBObject("keyWords" -> s".*${text}.*".r)),
            MongoDBObject("categories" -> s".*${text}.*".r)))
      } yield {
        mongoToProduct(p)
      })
  } catch {
    case e: Exception => fail(e)
  }

  def categoryById(id: String): Try[Category] = try {
    db("categories").findOne(MongoDBObject("_id.$oid" -> id)) match {
      case Some(obj) => Success(mongoToCategory(obj))
      case _ => fail("Item " + id + " not found")
    }
  } catch {
    case e: Exception => fail(e)
  }

  def allCategories: Try[Iterator[Category]] = try {
    Success(for { p <- db("categories").find() } yield {
      mongoToCategory(p)
    })
  } catch {
    case e: Exception => fail(e)
  }

  def createProducts(prod: ProductDetail*): Try[Seq[String]] = try {
    val mongos = prod.map(p => productToMongo(p))
    db("products").insert(mongos: _*)
    Success(mongos map { p => p.get("_id").getOrElse("?").toString() })
  } catch {
    case e: Exception => fail(e)
  }

  def createCategories(cats: Category*): Try[Seq[String]] = try {
    val mongos = cats.map(p => categoryToMongo(p))
    db("categories").insert(mongos: _*)
    Success(mongos map { p => p.getOrElse("_id", "?").toString })
  } catch {
    case e: Exception => fail(e)
  }

  private def productToMongo(obj: ProductDetail): MongoDBObject = {
    println(obj.categories)
    val db = MongoDBObject.newBuilder
    db += "title" -> MongoDBObject(obj.title.toList)
    db += "description" -> MongoDBObject(obj.description.toList)
    db += "properties" -> MongoDBObject(obj.properties.toList)
    db += ("price" -> obj.price)
    obj.oldPrice map { p => db += ("oldPrice" -> obj.oldPrice) }
    db += "soldCount" -> obj.soldCount
    db += "categories" -> obj.categories
    db += "images" -> obj.images
    db += "keyWords" -> obj.images
    db.result
  }

  private def categoryToMongo(obj: Category): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "title" -> MongoDBObject(obj.title.toList)
    db += ("image" -> obj.image)
    db.result
  }

  private def mongoToProduct(obj: DBObject): ProductDetail =
    ProductDetail(id = obj.getAs[ObjectId]("_id").map(_.toString),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      description = obj.getAsOrElse[Map[String, String]]("description", Map.empty),
      properties = obj.getAsOrElse[Map[String, String]]("properties", Map.empty),
      price = obj.getAsOrElse[Double]("price", 0.0),
      oldPrice = obj.getAs[Double]("oldPrice"),
      soldCount = obj.getAs[Int]("soldCOunt") getOrElse 0,
      categories = obj.getAsOrElse[List[String]]("categories", Nil),
      images = obj.getAsOrElse[List[String]]("images", Nil),
      keyWords = obj.getAsOrElse[List[String]]("keywords", Nil))

  private def mongoToCategory(obj: DBObject): Category =
    Category(id = obj.getAs[ObjectId]("_id").map(_.toString),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      image = obj.getAsOrElse[String]("image", "?"))
}
