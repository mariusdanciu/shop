package net.shop
package orders

import backend._
import net.shop.model.Order
import net.shift.common.Config
import net.shift.loc.Loc
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem

object MailObserver extends OrderObserver {
  implicit def stringToSeq(single: String): Seq[String] = Seq(single)

  val system = ActorSystem("idid")
  val mailActor = system.actorOf(Props[MailActor], "mailActor")

  def onOrder(content: OrderDocument) {
    mailActor ! Mail(
      from = Config.string("smtp.from"),
      to = content.o.email,
      bcc = Config.list("smtp.bcc"),
      subject = Loc.loc0(content.l)("order.subject").text,
      message = content.doc)
  }
}

case class Mail(
  from: String,
  to: Seq[String],
  cc: Seq[String] = Seq.empty,
  bcc: Seq[String] = Seq.empty,
  subject: String,
  message: String)

class MailActor extends Actor {

  def receive = {
    case mail: Mail => sendMail(mail)
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