package net.shop
package mongodb

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import net.shop.api.ProductDetail
import net.shop.api.Category
import org.bson.types.ObjectId
import net.shop.api.UserDetail
import net.shop.api.Address
import net.shop.api.UserInfo
import net.shop.api.CompanyInfo
import net.shop.api.Order
import net.shop.api.Person
import net.shop.api.Company
import net.shop.api.OrderLog
import net.shop.api.ProductLog
import java.util.Date
import net.shop.api.OrderStatus
import net.shop.api.Transport

trait MongoConversions {

  def orderToMongo(obj: OrderLog): DBObject = {
    val db = MongoDBObject.newBuilder
    val submitter = MongoDBObject.newBuilder
    obj submitter match {
      case Person(firstName, lastName, cnp) =>
        submitter += "firstName" -> firstName
        submitter += "lastName" -> lastName
        submitter += "cnp" -> cnp

      case Company(companyName, cif, regCom, bank, bankAccount) =>
        submitter += "companyName" -> companyName
        submitter += "cif" -> cif
        submitter += "regCom" -> regCom
        submitter += "bank" -> bank
        submitter += "bankAccount" -> bankAccount
    }

    db += "id" -> obj.id
    db += "time" -> obj.time
    db += "submitter" -> submitter.result
    db += "address" -> addressToMongo(obj.address)
    db += "email" -> obj.email
    db += "phone" -> obj.phone
    db += "transport" -> transportToMongo(obj.transport)

    val items = for { prod <- obj.items } yield {
      val item = MongoDBObject.newBuilder

      item += "id" -> prod.id
      item += "price" -> prod.price
      item += "quantity" -> prod.quantity
      item += "userOptions" -> prod.userOptions

      item.result
    }
    db += "items" -> items
    db += "status" -> obj.status.index
    db.result
  }

  def mongoToOrder(obj: DBObject): OrderLog = {
    val maybeCompany = obj.expand[String]("submitter.companyName")

    val submitterObj = maybeCompany match {
      case Some(comp) =>
        Company(companyName = comp,
          cif = obj.expand[String]("submitter.cif") getOrElse "",
          regCom = obj.expand[String]("submitter.regCom") getOrElse "",
          bank = obj.expand[String]("submitter.bank") getOrElse "",
          bankAccount = obj.expand[String]("submitter.bankAccount") getOrElse "")
      case None =>
        Person(firstName = obj.expand[String]("submitter.firstName") getOrElse "",
          lastName = obj.expand[String]("submitter.lastName") getOrElse "",
          cnp = obj.expand[String]("submitter.cnp") getOrElse "")
    }

    Address(
      name = obj.expand[String]("address.name") getOrElse "",
      country = obj.expand[String]("address.country") getOrElse "",
      region = obj.expand[String]("address.region") getOrElse "",
      city = obj.expand[String]("address.city") getOrElse "",
      address = obj.expand[String]("address.address") getOrElse "",
      zipCode = obj.expand[String]("address.zipCode") getOrElse "")

    val dbItems = obj.getAsOrElse[List[DBObject]]("items", Nil)

    val itemsObj = for { dbItem <- dbItems } yield {
      ProductLog(
        id = dbItem.getAsOrElse[String]("id", ""),
        price = dbItem.getAsOrElse[Double]("price", 0.0),
        quantity = dbItem.getAsOrElse[Int]("quantity", 0),
        userOptions = dbItem.getAsOrElse[Map[String, String]]("userOptions", Map.empty))
    }

    OrderLog(id = obj.getAsOrElse[String]("id", ""),
      time = obj.getAsOrElse[Date]("time", new Date),
      submitter = submitterObj,
      address = mongoToAddress(obj.getAsOrElse[DBObject]("address", MongoDBObject.newBuilder.result())),
      email = obj.getAsOrElse[String]("email", ""),
      phone = obj.getAsOrElse[String]("phone", ""),
      transport = mongoToTransport(obj.getAsOrElse[DBObject]("transport", MongoDBObject.newBuilder.result())),
      items = itemsObj,
      status = OrderStatus.fromIndex(obj.getAsOrElse[Int]("status", 0)))
  }

  def transportToMongo(obj: Transport): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "name" -> obj.name
    db += "price" -> obj.price
    db.result
  }

  def mongoToTransport(obj: DBObject): Transport =
    Transport(
      name = obj.getAsOrElse[String]("name", ""),
      price = obj.getAsOrElse[Double]("price", 0.0))

  def productToMongo(obj: ProductDetail): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "title" -> MongoDBObject(obj.title.toList)
    db += "description" -> MongoDBObject(obj.description.toList)
    db += "properties" -> MongoDBObject(obj.properties.toList)
    db += "options" -> MongoDBObject(obj.options.toList)
    db += "userText" -> obj.userText
    db += "price" -> obj.price
    db += "discountPrice" -> obj.discountPrice
    db += "soldCount" -> obj.soldCount
    db += "stock" -> obj.stock
    db += "position" -> obj.position
    db += "presentationPosition" -> obj.presentationPosition
    db += "unique" -> obj.unique
    db += "categories" -> obj.categories
    db += "images" -> obj.images
    db += "keywords" -> obj.keyWords
    db.result
  }

  def categoryToMongo(obj: Category): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "position" -> obj.position
    db += "title" -> MongoDBObject(obj.title.toList)
    db.result
  }

  def userToMongo(u: UserDetail): DBObject = {
    val db = MongoDBObject.newBuilder

    val userInfo = MongoDBObject.newBuilder
    userInfo += "firstName" -> u.userInfo.firstName
    userInfo += "lastName" -> u.userInfo.lastName
    userInfo += "cnp" -> u.userInfo.cnp
    userInfo += "phone" -> u.userInfo.phone

    val companyInfo = MongoDBObject.newBuilder
    companyInfo += "name" -> u.companyInfo.name
    companyInfo += "cif" -> u.companyInfo.cif
    companyInfo += "regCom" -> u.companyInfo.regCom
    companyInfo += "bank" -> u.companyInfo.bank
    companyInfo += "bankAccount" -> u.companyInfo.bankAccount
    companyInfo += "phone" -> u.companyInfo.phone

    db += "userInfo" -> userInfo.result
    db += "companyInfo" -> companyInfo.result
    db += "addresses" -> u.addresses.map(addressToMongo(_))
    db += "email" -> u.email
    db += "password" -> u.password
    db += "permissions" -> u.permissions
    db.result
  }

  def addressToMongo(addr: Address): DBObject = {
    val db = MongoDBObject.newBuilder
    db += "name" -> addr.name
    db += "country" -> addr.country
    db += "region" -> addr.region
    db += "city" -> addr.city
    db += "address" -> addr.address
    db += "zipCode" -> addr.zipCode
    db.result
  }

  import scala.collection.JavaConversions._

  def mongoToProduct(obj: DBObject): ProductDetail =
    ProductDetail(id = obj.getAs[ObjectId]("_id").map(_.toString),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      description = obj.getAsOrElse[Map[String, String]]("description", Map.empty),
      properties = obj.getAsOrElse[Map[String, String]]("properties", Map.empty),
      options = obj.getAs[Map[String, com.mongodb.BasicDBList]]("options").map { p =>
        p.map { case (k, v) => k -> v.toList.map { _.toString } }
      }.getOrElse(Map.empty),
      userText = obj.getAsOrElse[List[String]]("userText", Nil),
      price = obj.getAsOrElse[Double]("price", 0.0),
      discountPrice = obj.getAs[Double]("discountPrice"),
      soldCount = obj.getAs[Int]("soldCount") getOrElse 0,
      stock = obj.getAs[Int]("stock"),
      position = obj.getAs[Int]("position"),
      presentationPosition = obj.getAs[Int]("presentationPosition"),
      unique = obj.getAsOrElse[Boolean]("unique", false),
      categories = obj.getAsOrElse[List[String]]("categories", Nil),
      images = obj.getAsOrElse[List[String]]("images", Nil),
      keyWords = obj.getAsOrElse[List[String]]("keywords", Nil))

  def mongoToCategory(obj: DBObject): Category =
    Category(id = obj.getAs[ObjectId]("_id").map(_.toString),
      position = obj.getAsOrElse[Int]("position", 0),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty))

  def mongoToUser(obj: DBObject): UserDetail = {

    val ui = UserInfo(
      firstName = obj.expand[String]("userInfo.firstName").getOrElse(""),
      lastName = obj.expand[String]("userInfo.lastName").getOrElse(""),
      cnp = obj.expand[String]("userInfo.cnp").getOrElse(""),
      phone = obj.expand[String]("userInfo.phone").getOrElse(""))

    val ci = CompanyInfo(
      name = obj.expand[String]("companyInfo.name").getOrElse(""),
      cif = obj.expand[String]("companyInfo.cif").getOrElse(""),
      regCom = obj.expand[String]("companyInfo.regCom").getOrElse(""),
      bank = obj.expand[String]("companyInfo.bank").getOrElse(""),
      bankAccount = obj.expand[String]("companyInfo.bankAccount").getOrElse(""),
      phone = obj.expand[String]("companyInfo.phone").getOrElse(""))

    UserDetail(id = obj.getAs[ObjectId]("_id").map(_.toString),
      userInfo = ui,
      companyInfo = ci,
      addresses = obj.getAsOrElse[List[DBObject]]("addresses", Nil).map(mongoToAddress(_)),
      email = obj.getAsOrElse[String]("email", ""),
      password = obj.getAsOrElse[String]("password", ""),
      permissions = obj.getAsOrElse[List[String]]("permissions", Nil))
  }

  def mongoToAddress(obj: DBObject): Address =
    Address(id = obj.getAs[ObjectId]("_id").map(_.toString),
      name = obj.getAsOrElse[String]("name", ""),
      country = obj.getAsOrElse[String]("country", ""),
      region = obj.getAsOrElse[String]("region", ""),
      city = obj.getAsOrElse[String]("city", ""),
      address = obj.getAsOrElse[String]("address", ""),
      zipCode = obj.getAsOrElse[String]("zipCode", ""))

}
