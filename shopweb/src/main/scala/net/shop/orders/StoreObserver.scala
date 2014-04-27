package net.shop
package orders

import backend._
import scalax.file.Path
import net.shift.common.Log
import net.shop.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem

object StoreObserver extends OrderObserver {

  val system = ActorSystem("idid")
  val storeActor = system.actorOf(Props[StoreActor], "storeActor")

  def onOrder(content: OrderDocument) {
    storeActor ! StoreOrder(content)
    storeActor ! StoreOrderStats(content)
  }

}

sealed trait StoreMsg
case class StoreOrder(content: OrderDocument) extends StoreMsg
case class StoreOrderStats(content: OrderDocument) extends StoreMsg

class StoreActor extends Actor with Log {
  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  def receive = {
    case StoreOrder(content) => writeOrder(content)
    case StoreOrderStats(content) => writeStat(content.o)
  }

  def writeOrder(content: OrderDocument) {
    try {
      val path = Path.fromString(s"data/orders/${content.o.email}/${content.o.id}.html")
      path.write(content.doc)
    } catch {
      case e: Throwable => error("Cannot write order", e)
    }
  }

  def writeStat(o: Order) {
    try {
      val path = Path.fromString("data/stats/sales.csv")
      val date = new Date();
      val moment = dateFormat.format(date)

      for {
        i <- o.items
      } {
        path.append(List(o.email, moment, i._1 toString, i._2 toString) mkString ("", ",", "\n"))
      }

    } catch {
      case e: Throwable => e.printStackTrace(); error("Cannot write order", e)
    }

  }
}
