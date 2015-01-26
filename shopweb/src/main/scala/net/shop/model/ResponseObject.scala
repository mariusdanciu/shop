package net.shop
package model

object ResponseObjects {

}

case class ErrorMsg(error: String)
case class FieldError(id: String, error: String)
object ValidationFail {
  def apply(errors: FieldError*) = new ValidationFail(errors.toList)
}

case class ValidationFail(errors: List[FieldError]) {
  def append(other: ValidationFail) = ValidationFail(errors ::: other.errors)
}