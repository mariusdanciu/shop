package net.shop
package utils

import java.text.Normalizer

import net.shift.common.Config
import net.shop.api.persistence.Persistence
import net.shop.api.{Category, NamedItem, ProductDetail}

import scala.util.Random

object ShopUtils {

  val random = new Random(System.currentTimeMillis())
  val OBJECT_SUFFIX = "-handmade-lucrat-manual"

  def dataPath(implicit cfg: Config) = cfg.string("data.folder", "../data")

  def categoryImagePath(cat: Category): String = s"/data/categories/${cat.stringId}.png"

  def imagePath(id: String, variant: String, prod: String): String = s"/data/products/$id/$variant/$prod"

  def productPage(id: String)(implicit p: Persistence): String =
    p.productById(id) map { p => s"/product/${nameToPath(p)}" } getOrElse ("???")

  def productPage(p: ProductDetail): String = s"/product/${nameToPath(p)}"

  def imagePath(variant: String, prod: ProductDetail): String =
    prod.images match {
      case h :: _ => s"/data/products/${prod.stringId}/$variant/${h}"
      case Nil => ""
    }

  def productImages(variant: String, prod: ProductDetail): Seq[String] =
    prod.images map {
      p => s"/data/products/${prod.stringId}/$variant/${p}"
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
      .replaceAll("[\\p{InCombiningDiacriticalMarks}+\\p{Punct}]", "")
  }

}
