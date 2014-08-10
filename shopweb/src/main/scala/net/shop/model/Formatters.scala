package net.shop
package model

object Formatters {

  implicit object JsonOrderWriter extends Formatter[OrderLog] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(order: OrderLog): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(order)
    }

  }

}