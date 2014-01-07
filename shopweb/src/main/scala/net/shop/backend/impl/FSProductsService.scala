package net.shop
package backend
package impl

import java.io.FileInputStream

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.Exception._

import org.json4s._
import org.json4s.native.JsonMethods._

import scalax.file._
import scalax.file.PathMatcher._
import scalax.io._

class FSProductsService extends ProductsService {
  implicit val formats = DefaultFormats

  def productById(id: String): Try[ProductDetail] = {
    Try {
      Resource.fromInputStream(new FileInputStream(s"data/products/$id/data.json")).string
    } map { s =>
      parse(s).extract[ProductDetail]
    }
  }

  def allProducts(): Try[Traversable[ProductDetail]] = {
    val prods = (for {
      p <- Path.fromString(s"data/products").children().toList if (p.isDirectory)
    } yield {
      productById(p.simpleName)
    }).filter(_ isSuccess).map {
      case Success(p) => p
    }

    Success(prods)
  }

  def categoryProducts(cat: String): Try[Traversable[ProductDetail]] =
    (allProducts map { l => l.filter(p => p.categories.contains(cat)) }) match {
      case s @ Success(t) if (!t.isEmpty) => s
      case _ => Failure(new RuntimeException())
    }

  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]] = allProducts() match {
    case Success(prods) => Success(for (p <- prods if f(p)) yield p)
    case f => f
  }

  def categoryById(id: String): Try[Category] = {
    allCategories() match {
      case Success(l) => l.find(c => c.id == id) match {
        case Some(c) => Success(c)
        case _ => Failure(new RuntimeException(s"Category $id not found"))
      }
      case Failure(f) => Failure(f)
    }
  }

  def allCategories(): Try[Traversable[Category]] = {
    Try {
      Resource.fromInputStream(new FileInputStream(s"data/categories/categories.json")).string
    } map { s =>
      parse(s).extract[List[Category]]
    }
  }
}