package net.shop
package model

import net.shift.loc.Loc
import net.shift.loc.Language
import net.shift.io.FileSystem

object Formatters {

  implicit object ErrorJsonWriter extends Formatter[ShopError] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: ShopError)(implicit lang: Language, fs: FileSystem): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(Err(Loc.loc0(lang)(err.msg).text))
    }

    case class Err(msg: String)

  }

  implicit object JsonOrdersWriter extends Formatter[List[OrderLog]] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(orders: List[OrderLog])(implicit lang: Language, fs: FileSystem): String = {
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

    def write(user: UserDetail)(implicit lang: Language, fs: FileSystem): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(UserSummary(user.userInfo, user.email, user.addresses))
    }

    case class UserSummary(userInfo: UserInfo, email: String, addresses: List[Address])
  }

  implicit object ValidationErrorWriter extends Formatter[List[FieldError]] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: List[FieldError])(implicit lang: Language, fs: FileSystem): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(ValidationFail(err))
    }
  }

  implicit object ErrorWriter extends Formatter[FieldError] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(err: FieldError)(implicit lang: Language, fs: FileSystem): String = {
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

    def write(c: Category)(implicit lang: Language, fs: FileSystem): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(CategoryJson(Some(c.id), c.title.getOrElse(lang.name, "ro"), c.position))
    }

    case class CategoryJson(id: Option[String], title: String, position: Int)
  }

}