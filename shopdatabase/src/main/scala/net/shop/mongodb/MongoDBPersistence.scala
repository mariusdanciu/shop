package net.shop.mongodb

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

import net.shift.engine.ShiftFailure
import net.shop.model.Category
import net.shop.model.ProductDetail
import net.shop.persistence.NoSort
import net.shop.persistence.Persistence
import net.shop.persistence.SortSpec

object MongoDBPersistence extends Persistence {

  lazy val db = MongoClient("localhost")("idid")

  db.command(MongoDBObject("setParameter" -> 1, "textSearchEnabled" -> 1))

  db("products").ensureIndex(MongoDBObject("title.ro" -> 1))
  db("products").ensureIndex(MongoDBObject("keywords" -> 1))
  db("products").ensureIndex(MongoDBObject("categories" -> 1))
  
  def productById(id: String): Try[ProductDetail] = try {
    db("products").findOne(MongoDBObject("_id.$oid" -> id)) match {
      case Some(obj) => Success(mongoToProduct(obj))
      case _ => Failure(ShiftFailure("Item " + id + " not found"))
    }
  } catch {
    case e: Exception => Failure(e)
  }

  def allProducts: Try[Iterator[ProductDetail]] = try {
    Success(for { p <- db("products").find() } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => Failure(e)
  }

  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = try {
    Success(for { p <- db("products").find("categories" $in List(cat)) } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => Failure(e)
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
    case e: Exception => Failure(e)
  }

  def categoryById(id: String): Try[Category] = try {
    db("categories").findOne(MongoDBObject("_id.$oid" -> id)) match {
      case Some(obj) => Success(mongoToCategory(obj))
      case _ => Failure(ShiftFailure("Item " + id + " not found"))
    }
  } catch {
    case e: Exception => Failure(e)
  }

  def allCategories: Try[Iterator[Category]] = try {
    Success(for { p <- db("categories").find() } yield {
      mongoToCategory(p)
    })
  } catch {
    case e: Exception => Failure(e)
  }

  private def mongoToProduct(obj: DBObject): ProductDetail =
    ProductDetail(id = obj.getAs[ObjectId]("_id").get.toString,
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      price = obj.getAsOrElse[Double]("price", 0.0),
      oldPrice = obj.getAs[Double]("oldPrice"),
      categories = obj.getAsOrElse[List[String]]("categories", Nil),
      images = obj.getAsOrElse[List[String]]("images", Nil),
      keyWords = obj.getAsOrElse[List[String]]("keywords", Nil))

  private def mongoToCategory(obj: DBObject): Category =
    Category(id = obj.getAs[ObjectId]("_id").get.toString,
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      image = obj.getAsOrElse[String]("image", "?"))
}
