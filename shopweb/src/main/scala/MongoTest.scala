import net.shop.api._
import net.shop.api.persistence.SortSpec
import org.bson.BsonNull
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonDocument, BsonValue, DefaultBsonTransformers, ObjectId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object MongoTest extends App {

  println("Here")

  System.setProperty("org.mongodb.async.type", "netty")
  val uri = "mongodb://idid:qwer1234@idid-shard-00-00-ksqgy.mongodb.net:27017,idid-shard-00-01-ksqgy.mongodb.net:27017,idid-shard-00-02-ksqgy.mongodb.net:27017/?ssl=true&replicaSet=idid-shard-0&authSource=admin"

  val p = MongoPersistence(uri)

  //p.allProducts.map{
  //  s => println(s)
  //}

  p.productByName("ceas spiderman").map(println)

  Console.in.readLine()


}

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
      "_id" -> new ObjectId(),
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

  implicit def documentToProductFuture(d: Future[Document]): Future[ProductDetail] = d.map { e => e }

  implicit def documentToProduct(d: Document): ProductDetail =

    ProductDetail(
      id = Some(d.getObjectId("_id").toHexString),
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
      id = Some(d.getObjectId("_id").toHexString),
      name = d.getString("name"),
      position = d.getOrElse("position", 0).asInt32().getValue,
      title = d.getOrElse("title", BsonDocument()).asDocument()
    )


}


case class MongoPersistence(uri: String)(implicit ctx: ExecutionContext) {

  import MongoImplicits._
  import org.mongodb.scala.model.Filters._

  val client = MongoClient(uri)

  val db = client.getDatabase("idid")

  db.runCommand(Document("setParameter" -> 1, "textSearchEnabled" -> 1))

  val products = db.getCollection("products")

  val categories = db.getCollection("categories")

  products.createIndex(Document("title.ro" -> "text", "description.ro" -> "text", "keywords" -> "text"))


  def productById(id: String): Future[ProductDetail] = {
    products.find(equal("_id", new ObjectId(id))).first().toFuture()
  }

  def productByName(name: String): Future[ProductDetail] = {
    products.find(equal("name", name)).first().toFuture()
  }

  def allProducts: Future[Seq[ProductDetail]] = {
    products.find().toFuture().map {
      _.map { e => (e: ProductDetail) }
    }
  }

  def categoryProducts(cat: String, spec: SortSpec): Try[Iterator[ProductDetail]] = ???

  def searchProducts(text: String, spec: SortSpec): Try[Iterator[ProductDetail]] = ???

  def categoryByName(name: String): Try[Category] = ???

  def categoryById(id: String): Try[Category] = ???

  def allCategories: Try[Iterator[Category]] = ???

  def createProducts(prod: ProductDetail*): Try[Seq[String]] = ???

  def updateProducts(prod: ProductDetail*): Try[Seq[String]] = ???

  def deleteProducts(prod: String*): Try[Int] = ???

  def presentationProducts: Try[Seq[ProductDetail]] = ???

  def createCategories(prod: Category*): Try[Seq[String]] = ???

  def updateCategories(prod: Category*): Try[Seq[String]] = ???

  def deleteCategories(prod: String*): Try[Int] = ???

  def createUsers(user: UserDetail*): Try[Seq[String]] = ???

  def updateUsers(user: UserDetail*): Try[Seq[String]] = ???

  def deleteUserByEmail(email: String): Try[Int] = ???

  def deleteUsers(userId: String*): Try[Int] = ???

  def allUsers: Try[Iterator[UserDetail]] = ???

  def userByEmail(email: String): Try[Option[UserDetail]] = ???

  def createOrder(order: OrderLog*): Try[Seq[String]] = ???

  def ordersByEmail(email: String): Try[Iterator[OrderLog]] = ???

  def ordersByStatus(status: OrderStatus): Try[Iterator[OrderLog]] = ???

  def ordersByProduct(productId: String): Try[Iterator[OrderLog]] = ???

  def updateOrderStatus(orderId: String, status: OrderStatus): Try[Boolean] = ???
}