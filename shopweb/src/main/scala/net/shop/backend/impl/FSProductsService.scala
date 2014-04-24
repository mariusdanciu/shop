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
import net.shift.loc.Language

case class FSProductsService(lang: Language) extends ProductsService with TraversingSpec {
  implicit val formats = DefaultFormats

  override def productById(id: String): Try[ProductDetail] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/products/$id/data.json")).string
  } map { s =>
    parse(s).extract[ProductDetail]
  }

  override def allProducts: Try[Traversable[ProductDetail]] = {
    val l = (for {
      p <- Path.fromString(s"data/products").children().toList if (p.isDirectory)
    } yield {
      productById(p.simpleName)
    })
    listTraverse.sequence(l)
  }

  override def categoryProducts(cat: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]] =
    sort(allProducts match {
      case Success(all) =>
        Success(for {
          p <- all if (p.categories.contains(cat))
        } yield p)
      case f => f
    }, spec)

  override def categoryById(id: String): Try[Category] = {
    allCategories match {
      case Success(l) => l.find(c => c.id == id) match {
        case Some(c) => Success(c)
        case _ => Failure(new RuntimeException(s"Category $id not found"))
      }
      case Failure(f) => Failure(f)
    }
  }

  override def allCategories: Try[Traversable[Category]] = Try {
    Resource.fromInputStream(new FileInputStream(s"data/categories/categories.json")).string
  } map { s =>
    parse(s).extract[List[Category]]
  }

  override def searchProducts(text: String, spec: SortSpec = NoSort): Try[Traversable[ProductDetail]] = {
    sort((allProducts match {
      case Success(all) => Success(all filter predicate(text))
      case f => f
    }), spec)
  }

  def sort(in: => Try[Traversable[ProductDetail]], spec: SortSpec): Try[Traversable[ProductDetail]] = {
    spec match {
      case NoSort => in
      case SortByName(dir) => in.map(seq => seq.toList.sortWith((a, b) =>
        (for {
          l <- a.title.get(lang.language)
          r <- b.title.get(lang.language)
        } yield if (dir) l < r else l > r).getOrElse(false)))
      case SortByPrice(dir) => in.map(seq => seq.toList.sortWith((a, b) => if (dir) a.price < b.price else a.price > b.price))
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

}

