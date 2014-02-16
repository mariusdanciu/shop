package net.shop
package orders

import backend._
import scalax.file.Path

object StoreObserver extends OrderObserver {

  def onOrder(content: OrderDocument) {
    try {
      val path = Path.fromString(s"data/orders/${content.o.email}/${content.o.id}.html")
      path.write(content.doc)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

}