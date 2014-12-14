package net.shop
package persistence
package file

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
import net.shop.api.ProductDetail
import net.shop.api.Category
import net.shift.common.TraversingSpec
import net.shift.common.ApplicativeFunctor
import net.shift.engine.ShiftFailure
import net.shop.api.persistence.Persistence
import net.shop.api.persistence.NoSort
import net.shop.api.persistence.SortByName
import net.shop.api.persistence.SortByPrice
import net.shop.api.persistence.SortSpec

object FSProductsService extends Persistence with TraversingSpec {
  implicit val formats = DefaultFormats

  override def productById(id: String): Try[ProductDetail] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/products/$id/data.json")).string
  } map { s =>
    parse(s).extract[ProductDetail]
  }

  override def allProducts: Try[Iterator[ProductDetail]] = {
    val l = (for {
      p <- Path.fromString(s"data/products").children().toList if (p.isDirectory)
    } yield {
      productById(p.simpleName)
    })
    listTraverse.sequence(l) map { _.iterator }
  }

  override def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] =
    sort(allProducts match {
      case Success(all) =>
        Success(for {
          p <- all if (p.categories.contains(cat))
        } yield p)
      case f => f
    }, spec)

  override def categoryById(id: String): Try[Category] = {
    allCategories match {
      case Success(l) => l.find(c => c.stringId == id) match {
        case Some(c) => Success(c)
        case _ => Failure(new RuntimeException(s"Category $id not found"))
      }
      case Failure(f) => Failure(f)
    }
  }

  override def allCategories: Try[Iterator[Category]] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/categories/categories.json")).string
  } map { s =>
    parse(s).extract[List[Category]].iterator
  }

  override def searchProducts(text: String, spec: SortSpec = NoSort): Try[Iterator[ProductDetail]] = {
    sort((allProducts match {
      case Success(all) => Success(all filter predicate(text))
      case f => f
    }), spec)
  }

  private def sort(in: => Try[Iterator[ProductDetail]], spec: SortSpec): Try[Iterator[ProductDetail]] = {
    spec match {
      case NoSort => in
      case SortByName(dir, lang) => in.map(seq => seq.toList.sortWith((a, b) =>
        (for {
          l <- a.title.get(lang)
          r <- b.title.get(lang)
        } yield if (dir) l < r else l > r).getOrElse(false)).iterator)
      case SortByPrice(dir, lang) => in.map(seq => seq.toList.sortWith((a, b) => if (dir) a.price < b.price else a.price > b.price).iterator)
    }
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
  
  def createProducts(prod: ProductDetail*): Try[Seq[String]] = new ShiftFailure("Not supported")

  def updateProducts(prod: ProductDetail*): Try[Seq[String]] = new ShiftFailure("Not supported")
  
  def createCategories(cats: Category*): Try[Seq[String]] = new ShiftFailure("Not supported")

  def deleteProducts(prod: String*): Try[Int] = new ShiftFailure("Not supported")
}

