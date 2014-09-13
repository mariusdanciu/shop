package net.shop.orders

import net.shop.model.Order
import net.shift.loc.Language

object OrderSubmitter extends OrderObservable {

  var observers: List[OrderObserver] = Nil

  def accept(o: OrderObserver): OrderObservable = {
    observers = observers ++ List(o)
    this
  }

  def placeOrder(order: OrderDocument) {
    observers.foreach(_ onOrder order)
  }
}

trait OrderObservable {

  def accept(o: OrderObserver): OrderObservable

  def placeOrder(order: OrderDocument)
}

trait OrderObserver {

  def onOrder(o: OrderDocument)

}

case class OrderDocument(l: Language, o: Order, doc: String)