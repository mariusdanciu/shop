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
import net.shop.model.ProductDetail
import net.shop.model.Category
import net.shift.common.TraversingSpec
import net.shift.common.ApplicativeFunctor

class FSProductsService extends ProductsService with TraversingSpec {
  implicit val formats = DefaultFormats

  def productById(id: String): Try[ProductDetail] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/products/$id/data.json")).string
  } map { s =>
    parse(s).extract[ProductDetail]
  }

  def allProducts: Try[Traversable[ProductDetail]] = {
    val l = (for {
      p <- Path.fromString(s"data/products").children().toList if (p.isDirectory)
    } yield {
      productById(p.simpleName)
    })
    listTraverse.sequence(l)
  }
  def categoryProducts(cat: String): Try[Traversable[ProductDetail]] =
    allProducts match {
      case Success(all) =>
        Success(for {
          p <- all if (p.categories.contains(cat))
        } yield p)
      case f => f
    }

  def filter(f: ProductDetail => Boolean): Try[Traversable[ProductDetail]] = allProducts match {
    case Success(prods) => Success(for (p <- prods if f(p)) yield p)
    case f => f
  }

  def categoryById(id: String): Try[Category] = {
    allCategories match {
      case Success(l) => l.find(c => c.id == id) match {
        case Some(c) => Success(c)
        case _ => Failure(new RuntimeException(s"Category $id not found"))
      }
      case Failure(f) => Failure(f)
    }
  }

  def allCategories: Try[Traversable[Category]] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/categories/categories.json")).string
  } map { s =>
    parse(s).extract[List[Category]]
  }

  def searchProducts(text: String): Try[Traversable[ProductDetail]] = allProducts match {
    case Success(all) => Success(all filter predicate(text))
    case f => f
  }

  def predicate(text: String)(p: ProductDetail): Boolean = {
    var found = !(for (t <- p.title.values if t.toLowerCase().contains(text.toLowerCase())) yield t).isEmpty
    if (!found) {
      found = !(for {
        w <- text.split("\\s+")
        k <- p.keyWords ++ resolveCategories(p) if (k.toLowerCase().contains(w.toLowerCase()))
      } yield {
        w
      }).isEmpty
    }
    found
  }

  def resolveCategories(p: ProductDetail): List[String] = {
    p.categories.flatMap(c => categoryById(c) match {
      case Success(cat) => cat.title.values.toList
      case _ => Nil
    })
  }

}

