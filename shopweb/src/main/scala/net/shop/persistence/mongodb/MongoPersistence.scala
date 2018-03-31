package net.shop.persistence.mongodb

import net.shop.api._
import net.shop.api.persistence.{Persistence, SortByName, SortByPrice, SortSpec}
import org.bson.BsonNull
import org.mongodb.scala.{FindObservable, MongoClient, Observable}
import org.mongodb.scala.bson.{BsonDocument, BsonValue, DefaultBsonTransformers, ObjectId}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Sorts.{ascending, descending}
import org.mongodb.scala.model.TextSearchOptions

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object MongoImplicits extends DefaultBsonTransformers {

  import scala.collection.JavaConverters._

  implicit def docToMap(d: BsonDocument): Map[String, String] = {
    d.entrySet().asScala.map {
      case e => e.getKey -> e.getValue().asString().getValue
    } toMap
  }

  implicit def arrayToList(d: Seq[BsonValue]): List[String] = {
    d.map {
      _.asString().getValue
    } toList
  }

  implicit def productToDocument(p: ProductDetail): Document = {
    Document(
      "_id" -> p.id,
      "name" -> p.name,
      "title" -> Document(p.title),
      "description" -> Document(p.description),
      "properties" -> Document(p.properties),
      "price" -> p.price,
      "discountPrice" -> p.discountPrice,
      "soldCount" -> p.soldCount,
      "stock" -> p.stock,
      "position" -> p.position,
      "presentationPosition" -> p.presentationPosition,
      "unique" -> p.unique,
      "categories" -> p.categories,
      "keywords" -> p.keyWords
    )
  }

  def optionalInt(d: Document, name: String): Option[Int] = {
    d.get(name) match {
      case Some(_: BsonNull) => None
      case Some(v) => Some(v.asInt32().getValue)
      case _ => None
    }
  }


  def optionalDouble(d: Document, name: String): Option[Double] = {
    d.get(name) match {
      case Some(_: BsonNull) => None
      case Some(v) => Some(v.asDouble().getValue)
      case _ => None
    }
  }

  implicit def documentToProductFuture(d: Future[Document])(implicit cts: ExecutionContext): Future[ProductDetail] = d.map { e => e }

  implicit def documentToCategoryFuture(d: Future[Document])(implicit cts: ExecutionContext): Future[Category] = d.map { e => e }


  implicit def documentToProduct(d: Document): ProductDetail =

    ProductDetail(
      id = d.getObjectId("_id").toHexString,
      name = d.getString("name"),
      title = d.getOrElse("title", BsonDocument()).asDocument(),
      description = d.getOrElse("description", BsonDocument()).asDocument(),
      properties = d.getOrElse("properties", BsonDocument()).asDocument(),
      price = d.getOrElse("price", -1.0).asDouble().getValue,
      discountPrice = optionalDouble(d, "discountPrice"),
      soldCount = d.getOrElse("soldCount", 0).asInt32().getValue,
      stock = optionalInt(d, "stock"),
      position = optionalInt(d, "position"),
      presentationPosition = optionalInt(d, "presentationPosition"),
      unique = d.getOrElse("unique", false).asBoolean().getValue,
      categories = d.get("categories").map(_.asArray().getValues.asScala).getOrElse(Nil),
      keyWords = d.get("keywords").map(_.asArray().getValues.asScala).getOrElse(Nil)
    )

  implicit def categoryToDocument(p: Category): Document = {
    Document(
      "_id" -> new ObjectId(),
      "name" -> p.name,
      "position" -> p.position,
      "title" -> Document(p.title)
    )
  }


  implicit def documentToCategory(d: Document): Category =
    Category(
      id = d.getObjectId("_id").toHexString,
      name = d.getString("name"),
      position = d.getOrElse("position", 0).asInt32().getValue,
      title = d.getOrElse("title", BsonDocument()).asDocument()
    )


}


case class MongoPersistence(uri: String)(implicit ctx: ExecutionContext) extends Persistence {

  import MongoImplicits._
  import org.mongodb.scala.model.Filters._

  val client = MongoClient(uri)

  val db = client.getDatabase("idid")

  db.runCommand(Document("setParameter" -> 1, "textSearchEnabled" -> 1))

  val products = db.getCollection("products")

  val categories = db.getCollection("categories")

  products.createIndex(Document("title.ro" -> "text", "description.ro" -> "text", "keywords" -> "text"))

  private implicit def toTry[T](f: Future[T]): Try[T] = try {
    Success(Await.result(f, Duration.Inf))
  } catch {
    case e: Throwable => Failure(e)
  }

  private def sort[T](f: FindObservable[T], spec: SortSpec): Observable[T] = {
    spec match {
      case SortByName(true, _) => f.sort(ascending("name"))
      case SortByName(false, _) => f.sort(descending("name"))
      case SortByPrice(true, _) => f.sort(ascending("price"))
      case SortByPrice(false, _) => f.sort(descending("price"))
      case _ => f
    }
  }


  def productById(id: String): Try[ProductDetail] = toTry {
    (products.find(equal("_id", new ObjectId(id))).first().toFuture())
  }

  def productByName(name: String): Try[ProductDetail] = toTry {
    products.find(equal("name", name)).first().toFuture().map {
      c => c
    }
  }

  def allProducts: Try[Seq[ProductDetail]] = toTry {
    products.find().toFuture().map {
      _.map { e => (e: ProductDetail) }
    }
  }

  def categoryProducts(cat: String, spec: SortSpec): Try[Seq[ProductDetail]] = toTry {

    for {
      cat <- categories.find(equal("name", cat)).first().toFuture()
      prods <- {
        val c: Category = cat
        sort(products.find(in("categories", List(c.id))), spec).toFuture()
      }
    } yield {
      prods.map { p => (p: ProductDetail) }
    }
  }

  def searchProducts(s: String, spec: SortSpec): Try[Seq[ProductDetail]] = toTry {
    sort(products.find(text(s, TextSearchOptions()
      .caseSensitive(false)
      .diacriticSensitive(false)
      .language("english"))), spec).toFuture().map { s => s.map { e => (e: ProductDetail) } }
  }

  def categoryByName(name: String): Try[Category] = toTry {
    categories.find(equal("name", name)).first().toFuture()
  }

  def categoryById(id: String): Try[Category] = toTry {
    categories.find(equal("_id", id)).first().toFuture()
  }

  def allCategories: Try[Seq[Category]] = toTry {
    categories.find().toFuture().map {
      _.map { e => (e: Category) }
    }
  }

  def createProduct(prod: ProductDetail): Try[String] = toTry  {
    products.insertOne(prod).toFuture.map { _ => prod.id }
  }

  def updateProduct(prod: ProductDetail): Try[String] = toTry  {
    products.updateOne(equal("_id", prod.id), prod).toFuture().map { _ => prod.id }
  }

  def deleteProduct(id: String): Try[String] = toTry {
    products.deleteOne(equal("_id", id)).toFuture().map { _ => id }
  }

  def createCategory(cat: Category): Try[String] = toTry {
    categories.insertOne(cat).toFuture.map { _ => cat.id }
  }

  def updateCategory(cat: Category): Try[String] = toTry {
    categories.updateOne(equal("_id", cat.id), cat).toFuture().map { _ => cat.id}
  }

  def deleteCategory(id: String): Try[String] = toTry {
    categories.deleteOne(equal("_is", id)).toFuture().map { _ => id }
  }

  def createUsers(user: UserDetail*): Try[Seq[String]] = ???

  def updateUsers(user: UserDetail*): Try[Seq[String]] = ???

  def deleteUserByEmail(email: String): Try[Int] = ???

  def deleteUsers(userId: String*): Try[Int] = ???

  def allUsers: Try[Iterator[UserDetail]] = ???

  def userByEmail(email: String): Try[Option[UserDetail]] = ???

  def createOrder(order: OrderLog): Try[String] = {
    Future.successful("")
  }

  def ordersById(email: String): Try[Seq[OrderLog]] = ???

  def ordersByStatus(status: OrderStatus): Try[Seq[OrderLog]] = ???

  def ordersByProduct(productId: String): Try[Seq[OrderLog]] = ???

  def updateOrderStatus(orderId: String, status: OrderStatus): Try[String] = ???
}