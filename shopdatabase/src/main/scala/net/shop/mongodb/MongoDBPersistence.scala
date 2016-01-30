package net.shop
package mongodb

import scala.util.Success
import scala.util.Try
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import net.shift.common.Config
import net.shop.api._
import net.shop.api.persistence._
import ShopError._
import net.shop.api.ServiceHit

object MongoDBPersistence extends Persistence with MongoConversions {

  lazy val db = MongoClient(Config.string("db.host", "localhost"))("idid")

  db.command(MongoDBObject("setParameter" -> 1, "textSearchEnabled" -> 1))

  db("products").ensureIndex(MongoDBObject("title.ro" -> "text", "description.ro" -> "text", "keywords" -> "text"))
  db("products").ensureIndex(MongoDBObject("position" -> 1))
  db("users").ensureIndex(MongoDBObject("firstName" -> "text", "lastName" -> "text", "phone" -> "text", "email" -> "text"))
  db("orders").ensureIndex(MongoDBObject("email" -> 1, "items.id" -> 1))
  db("servicestats").ensureIndex(MongoDBObject("year" -> 1, "month" -> 1, "service" -> 1))

  def productById(id: String): Try[ProductDetail] = try {
    db("products").findOne(MongoDBObject("_id" -> new ObjectId(id))) match {
      case Some(obj) => Success(mongoToProduct(obj))
      case _         => fail("no.product")
    }
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def allProducts: Try[Iterator[ProductDetail]] = try {
    Success(for { p <- db("products").find() } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = try {

    val query = spec match {
      case SortByName(true, _)   => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> 1))
      case SortByName(false, _)  => db("products").find("categories" $in List(cat)).sort(MongoDBObject("title" -> -1))
      case SortByPrice(true, _)  => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> 1))
      case SortByPrice(false, _) => db("products").find("categories" $in List(cat)).sort(MongoDBObject("price" -> -1))
      case _                     => db("products").find("categories" $in List(cat)).sort(MongoDBObject("position" -> 1))
    }

    Success(for { p <- query } yield {
      mongoToProduct(p)
    })
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = try {
    val query = text match {
      case ":onsale" => db("products").find(MongoDBObject("discountPrice" -> MongoDBObject("$ne" -> None)))
      case t         => db("products").find(MongoDBObject("$text" -> MongoDBObject("$search" -> t)))
    }
    
    val sorted = spec match {
      case SortByName(true, _)   => query.sort(MongoDBObject("title" -> 1))
      case SortByName(false, _)  => query.sort(MongoDBObject("title" -> -1))
      case SortByPrice(true, _)  => query.sort(MongoDBObject("price" -> 1))
      case SortByPrice(false, _) => query.sort(MongoDBObject("price" -> -1))
      case _                     => query.sort(MongoDBObject("position" -> 1))
    }
    
    Success(
      for {
        p <- sorted
      } yield {
        mongoToProduct(p)
      })
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def presentationProducts: Try[Seq[ProductDetail]] = try {
    Success(
      (for {
        p <- db("products").find("presentationPosition" $gte 0).sort(MongoDBObject("presentationPosition" -> 1))
      } yield {
        mongoToProduct(p)
      }) toSeq)
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def categoryById(id: String): Try[Category] = try {
    db("categories").findOne(MongoDBObject("_id" -> new ObjectId(id))) match {
      case Some(obj) => Success(mongoToCategory(obj))
      case _         => fail("no.category")
    }
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def allCategories: Try[Iterator[Category]] = try {
    Success(for { p <- db("categories").find().sort(MongoDBObject("position" -> 1)) } yield {
      mongoToCategory(p)
    })
  } catch {
    case e: Exception => fail("internal.error")
  }

  def deleteProducts(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("products").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
  } catch {
    case e: Exception => fail("internal.error")
  }

  def createProducts(prod: ProductDetail*): Try[Seq[String]] = try {
    val mongos = prod.map(p => productToMongo(p))
    db("products").insert(mongos: _*)
    Success(mongos map { p => p.get("_id").getOrElse("?").toString() })
  } catch {
    case e: Exception => fail("internal.error")
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
    case e: Exception => fail("internal.error")
  }

  def createCategories(cats: Category*): Try[Seq[String]] = try {
    val mongos = cats.map(p => categoryToMongo(p))
    db("categories").insert(mongos: _*)
    Success(mongos map { p => p.getOrElse("_id", "?").toString })
  } catch {
    case e: Exception => fail("internal.error")
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
    case e: Exception => fail("internal.error")
  }

  def deleteCategories(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("categories").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
  } catch {
    case e: Exception => fail("internal.error")
  }

  def createUsers(user: UserDetail*): Try[Seq[String]] = try {
    val mongos = user.map(userToMongo(_))
    db("users").insert(mongos: _*)
    Success(mongos map { p => p.getOrElse("_id", "?").toString })
  } catch {
    case e: Exception => fail("internal.error")
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
    case e: Exception => fail("internal.error")
  }

  def deleteUsers(ids: String*): Try[Int] = try {
    val num = (0 /: ids)((acc, id) => db("users").remove(MongoDBObject("_id" -> new ObjectId(id))).getN)
    Success(num)
  } catch {
    case e: Exception => fail("internal.error")
  }

  def deleteUserByEmail(email: String): Try[Int] = try {
    Success(db("users").remove(MongoDBObject("email" -> email)).getN)
  } catch {
    case e: Exception => fail("internal.error")
  }

  def allUsers: Try[Iterator[UserDetail]] = try {
    Success(for { p <- db("users").find() } yield {
      mongoToUser(p)
    })
  } catch {
    case e: Exception => fail("internal.error")
  }

  def userByEmail(email: String): Try[Option[UserDetail]] = try {
    db("users").findOne(MongoDBObject("email" -> email)) match {
      case Some(obj) => Success(Some(mongoToUser(obj)))
      case _         => Success(None)
    }
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def createOrder(order: OrderLog*): Try[Seq[String]] = {
    val mongos = order.map { orderToMongo }
    try {
      db("orders").insert(mongos: _*)

      for {
        o <- order
        p <- o.items
      } {
        db("products").update(MongoDBObject("_id" -> new ObjectId(p.id)), $inc("soldCount" -> p.quantity))
      }

      Success(mongos map { _.getOrElse("_id", "?").toString })
    } catch {
      case e: Exception => fail("internal.error", e)
    }
  }

  def updateOrderStatus(orderId: String, status: OrderStatus): Try[Boolean] = try {
    val update = db("orders").update(MongoDBObject("id" -> orderId), $set(("status" -> status.index)))
    Success(update.getN == 1)
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def ordersByEmail(email: String): Try[Iterator[OrderLog]] = try {
    Success(
      db("orders").find(MongoDBObject("email" -> email)) map mongoToOrder)
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def ordersByStatus(status: OrderStatus): Try[Iterator[OrderLog]] = try {
    Success(db("orders").find(MongoDBObject("status" -> status.index)) map mongoToOrder)
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def ordersByProduct(productId: String): Try[Iterator[OrderLog]] = try {
    Success(
      db("orders").find("items.id" $in List(productId)) map mongoToOrder)
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def storeServiceHit(h: ServiceHit): Try[String] = try {
    val mongo = serviceHitToMongo(h)
    val update = $inc("count" -> 1)
    val result = db("servicestats").update(mongo, update, upsert = true)

    Success(mongo.get("_id").getOrElse("?").toString())
  } catch {
    case e: Exception => fail("internal.error", e)
  }

  def allServiceStats(): Try[Iterator[ServiceStat]] = try {
    Success(for { p <- db("servicestats").find() } yield {
      mongoToServiceStat(p)
    })
  } catch {
    case e: Exception => fail("internal.error", e)
  }

}

