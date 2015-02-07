package net.shop
package mongodb

import scala.util.Success
import scala.util.Try
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import net.shop.api._
import net.shop.api.persistence._
import net.shop.api.persistence.ShopError._
import net.shift.common.Config

object MongoDBPersistence extends Persistence with MongoConversions {

  lazy val db = MongoClient(Config.string("db.host", "localhost"))("idid")

  db.command(MongoDBObject("setParameter" -> 1, "textSearchEnabled" -> 1))

  db("products").ensureIndex(MongoDBObject("title.ro" -> "text", "description.ro" -> "text", "keywords" -> "text"))
  db("users").ensureIndex(MongoDBObject("firstName" -> "text", "lastName" -> "text", "phone" -> "text", "email" -> "text"))

  def productById(id: String): Try[ProductDetail] = try {
    db("products").findOne(MongoDBObject("_id" -> new ObjectId(id))) match {
      case Some(obj) => Success(mongoToProduct(obj))
      case _         => fail("Item " + id + " not found")
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
      case SortByName(true, _)   => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> 1))
      case SortByName(false, _)  => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> -1))
      case SortByPrice(true, _)  => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> 1))
      case SortByPrice(false, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> -1))
      case _                     => db("products").find("categories" $in List(cat))
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
        p <- db("products").find(MongoDBObject("$text" -> MongoDBObject("$search" -> text)))
      } yield {
        mongoToProduct(p)
      })
  } catch {
    case e: Exception => fail(e)
  }

  def categoryById(id: String): Try[Category] = try {
    db("categories").findOne(MongoDBObject("_id" -> new ObjectId(id))) match {
      case Some(obj) => Success(mongoToCategory(obj))
      case _         => fail("Item " + id + " not found")
    }
  } catch {
    case e: Exception => fail(e)
  }

  def allCategories: Try[Iterator[Category]] = try {
    Success(for { p <- db("categories").find().sort(MongoDBObject("position" -> 1)) } yield {
      mongoToCategory(p)
    })
  } catch {
    case e: Exception => fail(e)
  }

  def deleteProducts(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("products").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
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

  def updateProducts(prod: ProductDetail*): Try[Seq[String]] = try {
    val builder = db("products").initializeOrderedBulkOperation

    val ids = for {
      p <- prod
      id <- p.id
    } yield {
      builder.find(MongoDBObject("_id" -> new ObjectId(id))).update(MongoDBObject {
        "$set" -> productToMongo(p)
      })
      id
    }

    builder.execute()
    Success(ids)
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

  def updateCategories(c: Category*): Try[Seq[String]] = try {
    val builder = db("categories").initializeOrderedBulkOperation

    val ids = for {
      p <- c
      id <- p.id
    } yield {
      builder.find(MongoDBObject("_id" -> new ObjectId(id))).update(MongoDBObject {
        "$set" -> categoryToMongo(p)
      })
      id
    }

    builder.execute()
    Success(ids)
  } catch {
    case e: Exception => fail(e)
  }

  def deleteCategories(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("categories").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
  } catch {
    case e: Exception => fail(e)
  }

  def createUsers(user: UserDetail*): Try[Seq[String]] = try {
    val mongos = user.map(userToMongo(_))
    db("users").insert(mongos: _*)
    Success(mongos map { p => p.getOrElse("_id", "?").toString })
  } catch {
    case e: Exception => fail(e)
  }

  def updateUsers(user: UserDetail*): Try[Seq[String]] = try {
    val builder = db("users").initializeOrderedBulkOperation

    val ids = for {
      u <- user
      id <- u.id
    } yield {
      builder.find(MongoDBObject("_id" -> new ObjectId(id))).update(MongoDBObject {
        "$set" -> userToMongo(u)
      })
      id
    }

    builder.execute()
    Success(ids)
  } catch {
    case e: Exception => fail(e)
  }

  def deleteUsers(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("users").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
  } catch {
    case e: Exception => fail(e)
  }

  def allUsers: Try[Iterator[UserDetail]] = try {
    Success(for { p <- db("users").find() } yield {
      mongoToUser(p)
    })
  } catch {
    case e: Exception => fail(e)
  }

  def userByEmail(email: String): Try[UserDetail] = try {
    db("users").findOne(MongoDBObject("email" -> email)) match {
      case Some(obj) => Success(mongoToUser(obj))
      case _         => fail(email + " not found")
    }
  } catch {
    case e: Exception => fail(e)
  }

}
