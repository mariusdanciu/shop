package net.shop
package utils

import net.shop.api.ProductDetail
import net.shop.api.Category
import scala.util.Random
import net.shift.common.Config

object ShopUtils {
  val random = new Random(System.currentTimeMillis())

  def dataPath = Config.string("data.folder", "../data")
  
  def categoryImagePath(cat: Category): String = s"/data/categories/${cat.stringId}.png"

  def imagePath(id: String, variant: String, prod: String): String = s"/data/products/$id/$variant/$prod"

  def imagePath(variant: String, prod: ProductDetail): String =
    prod.images match {
      case h :: _ => s"/data/products/${prod.stringId}/$variant/${h}"
      case Nil    => "/static/images/noimage.png"
    }

  def errorTag(text: String) = <div class="error"><div><img src="/static/images/exclamation.png"/></div><span>{ text }</span></div>

  def uuid = ("" /: Range.apply(0, 7))((acc, v) => acc + random.nextInt(9))

}

