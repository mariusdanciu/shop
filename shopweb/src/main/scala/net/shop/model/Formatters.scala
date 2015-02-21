package net.shop
package model

import api._
import net.shift.loc.Language
import net.shop.api.ShopError

object Formatters {

  implicit object ErrorJsonWriter extends Formatter[ShopError] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: ShopError)(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(Err(err.msg))
    }

    case class Err(msg: String)

  }

  implicit object JsonOrdersWriter extends Formatter[List[OrderLog]] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(orders: List[OrderLog])(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(orders)
    }

  }

  implicit object UserDetailWriter extends Formatter[UserDetail] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(user: UserDetail)(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(UserSummary(user.userInfo, user.companyInfo, user.email, user.addresses))
    }

    case class UserSummary(userInfo: UserInfo, companyInfo: CompanyInfo, email: String, addresses: List[Address])
  }

  implicit object ValidationErrorWriter extends Formatter[ValidationFail] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: ValidationFail)(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(err)
    }
  }

  implicit object ErrorWriter extends Formatter[FieldError] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: FieldError)(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(err)
    }
  }

  implicit object CategoryWriter extends Formatter[Category] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(c: Category)(implicit lang: String): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(CategoryJson(c.id, c.title.getOrElse(lang, "ro"), c.position))
    }

    case class CategoryJson(id: Option[String], title: String, position: Int)
  }

}