package net.shop
package backend
package impl

import scala.util.Try
import scala.util.control.Exception._
import org.json4s._
import org.json4s.native.JsonMethods._
import scalax.file._
import scalax.io._
import scalax.file.PathMatcher._
import scala.util.Success
import scala.util.Failure


class FSProductsService extends ProductsService {
  implicit val formats = DefaultFormats
  
  def byId(id: String): Try[ProductDetail] = {
    catching(classOf[java.io.FileNotFoundException]).withTry {
      Resource.fromFile("data/products/" + id + "/data.json").string
    } map {s => 
    	 parse(s).extract[ProductDetail]
    }
  }
  
  def allProducts(): Try[List[ProductDetail]] = {
    val prods = (for { 
      p <- Path.fromString("data/products").children().toList if (p.isDirectory)
    } yield {
      byId(p.simpleName)
    }).filter(_ isSuccess).map {
      case Success(ProductDetail(id, title, _, price, images)) => ProductDetail(id, title, price, images)
    }
    
    Success(prods)
  }
}