package net.shop
package model

import api._

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

  implicit object UserDetailWriter extends Formatter[UserDetail] {
    import org.json4s._
    import org.json4s.native.Serialization

    implicit val formats = new DefaultFormats {
      override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }

    def write(user: UserDetail): String = {
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

    def write(err: ValidationFail): String = {
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

    def write(err: FieldError): String = {
      import org.json4s.native.Serialization.writePretty
      writePretty(err)
    }
  }

}