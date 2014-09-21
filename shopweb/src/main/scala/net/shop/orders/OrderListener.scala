package net.shop
package orders

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import net.shift.common.Config
import net.shift.common.DefaultLog
import net.shift.common.Log
import net.shift.loc._
import net.shop.api._
import net.shop.model.Formatters._

object OrderListener extends OrderObserver {
  implicit def stringToSeq(single: String): Seq[String] = Seq(single)

  val system = ActorSystem("idid")
  val orderActor = system.actorOf(Props[OrderActor], "orderActor")

  def onOrder(content: OrderDocument) {
    orderActor ! StoreOrderStats(content)
    orderActor ! Mail(
      from = Config.string("smtp.from"),
      to = content.o.email,
      bcc = Config.list("smtp.bcc"),
      subject = Loc.loc0(content.l)("order.subject").text,
      message = content.doc)
  }

}

sealed trait OrderMsg
case class StoreOrderStats(content: OrderDocument) extends OrderMsg
case class Mail(
  from: String,
  to: Seq[String],
  cc: Seq[String] = Seq.empty,
  bcc: Seq[String] = Seq.empty,
  subject: String,
  message: String) extends OrderMsg

class OrderActor extends Actor with DefaultLog {

  val statsLog = new Log {
    def loggerName = "stats"
  }

  def receive = {
    case StoreOrderStats(content) => writeStat(content.o)
    case mail: Mail => sendMail(mail)
  }

  def writeStat(o: Order) {
    statsLog.info(Formatter.format(o.toOrderLog) + ",")
  }

  def sendMail(mail: Mail) {
    import org.apache.commons.mail._

    val commonsMail: Email = new HtmlEmail().setHtmlMsg(mail.message)
    mail.to foreach (commonsMail.addTo(_))
    mail.cc foreach (commonsMail.addCc(_))
    mail.bcc foreach (commonsMail.addBcc(_))

    commonsMail.setHostName(Config.string("smtp.server"))
    commonsMail.setSmtpPort(Config.int("smtp.port"))
    commonsMail.setAuthenticator(new DefaultAuthenticator(Config.string("smtp.user"), Config.string("smtp.password")))
    commonsMail.setSSLOnConnect(true);
    commonsMail.setFrom(mail.from).
      setSubject(mail.subject).
      send()
  }
}


