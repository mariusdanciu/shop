package net.shop
package utils

import java.text.Normalizer

import net.shift.common.{Config, Path}
import net.shift.io.FileSystem
import net.shop.model.{Category, NamedItem, ProductDetail}
import net.shop.persistence.Persistence

import scala.util.{Random, Try}

trait ProductImageVariant {
  val name: String
}

case object ThumbPic extends ProductImageVariant {
  val name = "thumb"
}

case object NormalPic extends ProductImageVariant {
  val name = "normal"
}

case object LargePic extends ProductImageVariant {
  val name = "large"
}

object ShopUtils {

  val random = new Random(System.currentTimeMillis())
  val OBJECT_SUFFIX = "-handmade-lucrat-manual"

  def dataPath(implicit cfg: Config) = cfg.string("data.folder", "../data")

  def categoryImagePath(cat: Category): String = s"/data/categories/${cat.stringId}.png"

  def productPage(id: String)(implicit p: Persistence): Try[String] =
    p.productById(id) map { p => s"/product/${nameToPath(p)}" }

  def productPage(p: ProductDetail): String = s"/product/${nameToPath(p)}"

  def prodImageFiles(prodId: String)(implicit fs: FileSystem, cfg: Config): Seq[String] = {
    val path = s"$dataPath/products/${prodId}/${ThumbPic.name}"
    fs.ls(Path(path)).map { s =>
      s.map {
        _.toString
      }
    } getOrElse Nil
  }

  def imagePath(variant: ProductImageVariant, prodId: String, file: String)(implicit fs: FileSystem, cfg: Config): String =
    s"/data/products/${prodId}/${variant.name}/$file"

  def imagePath(variant: ProductImageVariant, prodId: String)(implicit fs: FileSystem, cfg: Config): String =
    prodImageFiles(prodId) match {
      case h :: _ => s"/data/products/${prodId}/${variant.name}/${h}"
      case Nil => ""
    }


  def imagePath(variant: ProductImageVariant, prod: ProductDetail)(implicit fs: FileSystem, cfg: Config): String =
    imagePath(variant, prod.stringId)

  def productImages(variant: ProductImageVariant, prod: ProductDetail)(implicit fs: FileSystem, cfg: Config): Seq[String] =
    prodImageFiles(prod.stringId) map {
      p => s"/data/products/${prod.stringId}/${variant.name}/${p}"
    }

  def errorTag(text: String) = <div class="error">
    <div>
      <span class="sprite sprite-exclamation"/>
    </div> <span>
      {text}
    </span>
  </div>

  def uuid = ("" /: Range.apply(0, 7)) ((acc, v) => acc + random.nextInt(9))

  def price(p: Double) = if ((p % 1) == 0) "%.0f" format p else "%.2f" format p

  def extractName(name: String): String = {
    val idx = name.indexOf(OBJECT_SUFFIX)
    if (idx < 0)
      name
    else
      name.substring(0, idx).replaceAll("-", " ")
  }

  def nameToPath(item: NamedItem): String = {
    val n = item.name.replaceAll(" ", "-")
    n + OBJECT_SUFFIX
  }

  def normalizeName(name: String): String = {
    Normalizer.normalize(name, Normalizer.Form.NFD)
      .replaceAll("[\\p{InCombiningDiacriticalMarks}+\\p{Punct}]", "").toLowerCase
  }

}
