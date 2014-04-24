package net.shop
package utils

import net.shop.model.ProductDetail
import net.shop.model.Category

object ShopUtils {

  
  def categoryImagePath(cat: Category): String = s"/data/categories/${cat.image}"
  
  def imagePath(id: String, prod: String): String = s"/data/products/$id/$prod"

  def imagePath(id: String, variant: String, prod: String): String = s"/data/products/$id/$variant/$prod"

  def imagePath(prod: ProductDetail): String = s"/data/products/${prod.id}/${prod.images.head}"

  def imagePath(variant:String, prod: ProductDetail): String = s"/data/products/${prod.id}/$variant/${prod.images.head}"
  
  def errorTag(text: String) = <div class="error"><div><img src="/static/images/exclamation.png"/></div><span>{ text }</span></div>
}