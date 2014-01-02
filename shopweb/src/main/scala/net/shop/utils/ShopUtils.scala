package net.shop
package utils

import net.shop.backend.ProductDetail
import net.shop.backend.Category

object ShopUtils {

  def productImagePath(prod: ProductDetail): String = s"/data/products/${prod.id}/${prod.images.head}"
  
  def categoryImagePath(cat: Category): String = s"/data/categories/${cat.image}"
  
  def augmentImagePath(id: String, prod: String): String = s"/data/products/$id/$prod"
}