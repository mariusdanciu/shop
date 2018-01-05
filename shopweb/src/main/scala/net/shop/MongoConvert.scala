package net.shop

import org.bson.{BsonDocument, BsonValue}
import org.mongodb.scala.bson.{BsonDocument, Document}

import scala.collection.JavaConverters._
import scala.util.Try

object MongoConvert {

  def documentToMap(doc: BsonDocument): Map[String, String] = {
    doc.entrySet().asScala.map { e =>
      (e.getKey, e.getValue.asString().getValue)
    }.toMap
  }

  def getMap(key: String, doc: Document) =
    doc.get[BsonDocument](key).map {
      MongoConvert.documentToMap(_)
    } getOrElse Map.empty


  def productToDocument(p: ProductDetail): Try[Document] = Try {
    Document(
      "name" -> p.name.toLowerCase(),
      "title" -> p.title.toList,
      "description" -> p.description.toList,
      "properties" -> p.properties.toList,
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

  /*
    def documentToProduct(d: Document): Try[ProductDetail] = Try {
      val obj = d.toMap

      val t = d.get[Document]("title").map

      ProductDetail(
        id = Some(d.getObjectId("_id").toHexString),
        name = d.getString("name"),
        title = d.get[Document]("title"),
        description = obj.underlying.get("description"),
        properties = obj.underlying.get("properties"),
        price = obj.underlying.getDouble("price", 0.0),
        discountPrice = obj.underlying.getDouble("discountPrice"),
        soldCount = obj.underlying.getInteger("soldCount", 0),
        stock = obj.underlying.getInteger("stock", 0),
        position = obj.underlying.getInteger("position", 0),
        presentationPosition = obj.underlying.getInteger("presentationPosition", 0),
        unique = obj.underlying.getBoolean("unique"),
        categories = obj.underlying.getArray("categories").toArray,
        keyWords = obj.underlying.getArray("keywords").toArray
      )
    }
    */
}
