package net.shop
package api

import java.util.Date
import scala.util.Failure
import net.shift.security.User
import net.shift.security.Permissions
import net.shift.security.Permission

case class UserInfo(firstName: String, lastName: String, cnp: String, phone: String)
case class CompanyInfo(name: String, cif: String, regCom: String, bank: String, bankAccount: String, phone: String)

case class UserDetail(id: Option[String] = None,
                      userInfo: UserInfo,
                      companyInfo: CompanyInfo,
                      addresses: List[Address],
                      email: String,
                      password: String,
                      permissions: List[String]) {
  lazy val securityUser: User = User(email, None, permissions.map(Permission(_)).toSet)
}

case class Address(id: Option[String] = None,
                   name: String,
                   country: String,
                   region: String,
                   city: String,
                   address: String,
                   zipCode: String)

case class ProductDetail(id: Option[String] = None,
                         title: Map[String, String],
                         description: Map[String, String],
                         properties: Map[String, String],
                         price: Double,
                         discountPrice: Option[Double],
                         soldCount: Int,
                         unique: Boolean,
                         stock: Option[Int],
                         categories: List[String],
                         images: List[String],
                         keyWords: List[String]) {

  def stringId = id getOrElse "?"

  def title_?(l: String) = title.getOrElse(l, "???")

  def toProductLog(quantity: Int) = ProductLog(stringId, price, quantity)
}

case class CartItem(id: String, count: Int)

case class Cart(items: List[CartItem])

case class Category(id: Option[String] = None, position: Int, title: Map[String, String]) {
  def title_?(l: String) = title.getOrElse(l, "???")
  def stringId = id getOrElse "?"
}

sealed trait Submitter

case class Person(firstName: String, lastName: String, cnp: String) extends Submitter
case class Company(companyName: String, cif: String, regCom: String, bank: String, bankAccount: String) extends Submitter

case class Order(id: String,
                 submitter: Submitter,
                 address: Address,
                 email: String,
                 phone: String,
                 terms: Boolean,
                 items: List[(ProductDetail, Int)]) {

  def toOrderLog = OrderLog(id, new Date(), submitter, address, email, phone, items map { i => i._1.toProductLog(i._2) })
}

object OrderStatus {
  def fromIndex(v: Int) = v match {
    case 0 => OrderReceived
    case 1 => OrderPending
    case 2 => OrderFinalized
    case _ => OrderCanceled
  }
}

sealed trait OrderStatus {
  def index: Int
}
case object OrderReceived extends OrderStatus {
  def index = 0
}
case object OrderPending extends OrderStatus {
  def index = 1
}
case object OrderFinalized extends OrderStatus {
  def index = 2
}
case object OrderCanceled extends OrderStatus {
  def index = -1
}

case class OrderLog(id: String,
                    time: Date,
                    submitter: Submitter,
                    address: Address,
                    email: String,
                    phone: String,
                    items: List[ProductLog],
                    status: OrderStatus = OrderReceived) {

  lazy val total = (0.0 /: items)((a, i) => a + i.price * i.quantity)
}

case class ProductLog(id: String, price: Double, quantity: Int)

case class ServiceHit(year: Int, month: Int, day: Int, service: String)
case class ServiceStat(hit: ServiceHit, count: Long)

object Formatter {
  def format[T: Formatter](v: T)(implicit lang: String): String = {
    implicitly[Formatter[T]].write(v)
  }
}

trait Formatter[T] {
  def write(value: T)(implicit lang: String): String
}

object ShopError {
  def fail(msg: String) = Failure(new ShopError(msg))
  def fail(e: Throwable) = Failure(new ShopError(e))
}

case class ShopError(msg: String, e: Throwable) extends RuntimeException(msg, e) {
  def this(msg: String) = this(msg, null)
  def this(e: Throwable) = this(e.getMessage(), e)
}

