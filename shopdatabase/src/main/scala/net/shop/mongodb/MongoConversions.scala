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

trait MongoConversions {
  def productToMongo(obj: ProductDetail): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "title" -> MongoDBObject(obj.title.toList)
    db += "description" -> MongoDBObject(obj.description.toList)
    db += "properties" -> MongoDBObject(obj.properties.toList)
    db += "price" -> obj.price
    db += "discountPrice" -> obj.discountPrice
    db += "soldCount" -> obj.soldCount
    db += "categories" -> obj.categories
    db += "images" -> obj.images
    db += "keywords" -> obj.keyWords
    db.result
  }

  def categoryToMongo(obj: Category): MongoDBObject = {
    val db = MongoDBObject.newBuilder
    db += "title" -> MongoDBObject(obj.title.toList)
    obj.image.map(img => db += ("image" -> img))
    db.result
  }

  def userToMongo(u: UserDetail): DBObject = {
    val db = MongoDBObject.newBuilder

    val userInfo = MongoDBObject.newBuilder
    userInfo += "firstName" -> u.userInfo.firstName
    userInfo += "lastName" -> u.userInfo.lastName
    userInfo += "cnp" -> u.userInfo.cnp
    userInfo += "phone" -> u.userInfo.phone

    val companyInfo = u.companyInfo.map { ci =>
      val companyInfo = MongoDBObject.newBuilder
      companyInfo += "name" -> ci.name
      companyInfo += "cif" -> ci.cif
      companyInfo += "regCom" -> ci.regCom
      companyInfo += "bank" -> ci.bank
      companyInfo += "bankAccount" -> ci.bankAccount
      companyInfo += "phone" -> ci.phone
      companyInfo.result()
    }
    db += "userInfo" -> userInfo.result
    db += "companyInfo" -> companyInfo
    db += "addresses" -> u.addresses.map(addressToMongo(_))
    db += "email" -> u.email
    db += "password" -> u.password
    db += "permissions" -> u.permissions
    db.result
  }

  def addressToMongo(addr: Address): DBObject = {
    val db = MongoDBObject.newBuilder
    db += "country" -> addr.country
    db += "region" -> addr.region
    db += "city" -> addr.city
    db += "address" -> addr.address
    db += "zipCode" -> addr.zipCode
    db.result
  }
  def mongoToProduct(obj: DBObject): ProductDetail =
    ProductDetail(id = obj.getAs[ObjectId]("_id").map(_.toString),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      description = obj.getAsOrElse[Map[String, String]]("description", Map.empty),
      properties = obj.getAsOrElse[Map[String, String]]("properties", Map.empty),
      price = obj.getAsOrElse[Double]("price", 0.0),
      discountPrice = obj.getAs[Double]("discountPrice"),
      soldCount = obj.getAs[Int]("soldCOunt") getOrElse 0,
      categories = obj.getAsOrElse[List[String]]("categories", Nil),
      images = obj.getAsOrElse[List[String]]("images", Nil),
      keyWords = obj.getAsOrElse[List[String]]("keywords", Nil))

  def mongoToCategory(obj: DBObject): Category =
    Category(id = obj.getAs[ObjectId]("_id").map(_.toString),
      title = obj.getAsOrElse[Map[String, String]]("title", Map.empty),
      image = obj.getAs[String]("image"))

  def mongoToUser(obj: DBObject): UserDetail = {

    val ui = UserInfo(
      firstName = obj.getAsOrElse[String]("userInfo.firstName", ""),
      lastName = obj.getAsOrElse[String]("userInfo.lastName", ""),
      cnp = obj.getAsOrElse[String]("userInfo.cnp", ""),
      phone = obj.getAs[String]("userInfo.phone"))

    val ci = obj.getAs[String]("companyInfo.name").map(name =>
      CompanyInfo(
        name = name,
        cif = obj.getAsOrElse[String]("companyInfo.cif", ""),
        regCom = obj.getAsOrElse[String]("companyInfo.regCom", ""),
        bank = obj.getAsOrElse[String]("companyInfo.bank", ""),
        bankAccount = obj.getAsOrElse[String]("companyInfo.bankAccount", ""),
        phone = obj.getAs[String]("companyInfo.phone")))

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
      country = obj.getAsOrElse[String]("country", ""),
      region = obj.getAsOrElse[String]("region", ""),
      city = obj.getAsOrElse[String]("city", ""),
      address = obj.getAsOrElse[String]("address", ""),
      zipCode = obj.getAsOrElse[String]("zipCode", ""))

}
