package net.shop

import java.util.Date

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.{Document, ObjectId}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoDatabase, Observer}


object Category {
  def apply(name: String,
            position: Int,
            title: Map[String, String]): Category = Category(None, name, position, title);
}

case class Category(_id: Option[ObjectId],
                    name: String,
                    position: Int,
                    title: Map[String, String])

object ProductDetail {
  def apply(name: String,
            title: Map[String, String],
            description: Map[String, String],
            properties: Map[String, String],
            price: Double,
            discountPrice: Option[Double],
            soldCount: Int,
            position: Option[Int],
            presentationPosition: Option[Int],
            unique: Option[Boolean],
            stock: Option[Int],
            categories: List[String],
            keyWords: Option[List[String]]): ProductDetail =
    ProductDetail(None,
      name,
      title,
      description,
      properties,
      price,
      discountPrice,
      soldCount,
      position,
      presentationPosition,
      unique,
      stock,
      categories,
      keyWords
    )
}

case class ProductDetail(_id: Option[ObjectId],
                         name: String,
                         title: Map[String, String],
                         description: Map[String, String],
                         properties: Map[String, String],
                         price: Double,
                         discountPrice: Option[Double],
                         soldCount: Int,
                         position: Option[Int],
                         presentationPosition: Option[Int],
                         unique: Option[Boolean],
                         stock: Option[Int],
                         categories: List[String],
                         keyWords: Option[List[String]])

case class UserInfo(firstName: String, lastName: String, cnp: String, phone: String)

case class CompanyInfo(name: String, cif: String, regCom: String, bank: String, bankAccount: String, phone: String)

case class Address(name: String,
                   country: String,
                   region: String,
                   city: String,
                   address: String,
                   zipCode: String)

case class UserDetail(_id: Option[ObjectId] = None,
                      userInfo: UserInfo,
                      addresses: List[Address],
                      email: String,
                      password: String,
                      permissions: List[String])


case class Person(firstName: String, lastName: String, cnp: String)

case class Company(companyName: String, cif: String, regCom: String, bank: String, bankAccount: String)

case class Transport(name: String, price: Double)

case class ProductLog(id: String, price: Double, quantity: Int)


// OrderLog(54e89000e4b08d558b437933,Sat Feb 21 16:02:40 EET 2015,None,None,Address(destination,Romania,qweqwe,qwe,qweq,123123),marius.danciu@gmail.com,234235423,None,List(ProductLog(54e05a94e4b0643b17db7365,65.0,1)),2)
// OrderLog(4773878,Sat Feb 21 16:02:40 EET 2015,None,None,Address(destination,Romania,qweqwe,qwe,qweq,123123),marius.danciu@gmail.com,234235423,None,List(ProductLog(54e05a94e4b0643b17db7365,65.0,1)),2)
// Document((_id,BsonObjectId{value=54e89000e4b08d558b437933}), (id,BsonString{value='4773878'}), (time,BsonDateTime{value=1424527360656}), (submitter,{ "firstName" : "asdihoih", "lastName" : "oioij", "cnp" : "23423423423" }), (address,{ "name" : "destination", "country" : "Romania", "region" : "qweqwe", "city" : "qwe", "address" : "qweq", "zipCode" : "123123" }), (email,BsonString{value='marius.danciu@gmail.com'}), (phone,BsonString{value='234235423'}), (items,BsonArray{values=[{ "id" : "54e05a94e4b0643b17db7365", "price" : 65.0, "quantity" : 1 }]}), (status,BsonInt32{value=2}))

case class OrderLog(id: String,
                    time: Date,
                    person: Option[Person],
                    company: Option[Company],
                    address: Address,
                    email: String,
                    phone: String,
                    transport: Option[Transport],
                    items: List[ProductLog],
                    status: Int = 0)

object MongoTest extends App {

  val codecRegistry = fromRegistries(fromProviders(classOf[Category]),
    fromProviders(classOf[ProductDetail]),
    fromProviders(classOf[UserDetail]),
    fromProviders(classOf[UserInfo]),
    fromProviders(classOf[Address]),
    fromProviders(classOf[OrderLog]),
    fromProviders(classOf[ProductLog]),
    fromProviders(classOf[Person]),
    fromProviders(classOf[Company]),
    fromProviders(classOf[Transport]),
    DEFAULT_CODEC_REGISTRY)

  println("Here")

  System.setProperty("org.mongodb.async.type", "netty")
  val uri = "mongodb://idid:qwer1234@idid-shard-00-00-ksqgy.mongodb.net:27017,idid-shard-00-01-ksqgy.mongodb.net:27017,idid-shard-00-02-ksqgy.mongodb.net:27017/?ssl=true&replicaSet=idid-shard-0&authSource=admin"

  val client = MongoClient(uri)

  val db: MongoDatabase = client.getDatabase("idid")
    .withCodecRegistry(codecRegistry)

  db.listCollectionNames().subscribe(new Observer[String]() {
    override def onNext(result: String): Unit = println(result)

    override def onError(e: Throwable): Unit = e.printStackTrace()

    override def onComplete(): Unit = println("completed")
  })

  val categories = db.getCollection[Category]("categories")

  categories.find().subscribe(new Observer[Category]() {
    override def onNext(result: Category): Unit = {
      println(result)
    }

    override def onError(e: Throwable): Unit = e.printStackTrace()

    override def onComplete(): Unit = println("completed")
  })

  val products = db.getCollection[ProductDetail]("products")


  products.find().first().subscribe(new Observer[ProductDetail]() {
    override def onNext(result: ProductDetail): Unit = {
      println(result)
    }

    override def onError(e: Throwable): Unit = e.printStackTrace()

    override def onComplete(): Unit = println("completed")
  })

  val users = db.getCollection[UserDetail]("users")


  users.find().first().subscribe(new Observer[UserDetail]() {
    override def onNext(result: UserDetail): Unit = {
      println(result)
    }

    override def onError(e: Throwable): Unit = e.printStackTrace()

    override def onComplete(): Unit = println("completed")
  })


  val orders = db.getCollection("orders")


  orders.find().first().subscribe(new Observer[Document]() {
    override def onNext(result: Document): Unit = {
      result.foreach{ case (id, bson) => println(id + " - " + bson)}
      println(result)
    }

    override def onError(e: Throwable): Unit = e.printStackTrace()

    override def onComplete(): Unit = println("completed")
  })


  Console.in.readLine()
  client.close()
}

