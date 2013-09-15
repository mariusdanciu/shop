package net.shop
package utils

import net.shop.backend.ProductDetail

object ShopUtils {

  def productImagePath(prod: ProductDetail): String = "data/products/" + prod.id + "/" + prod.images.head
}