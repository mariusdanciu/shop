package net.shop.messaging

import net.shop.api.Order
import net.shift.loc.Language
import akka.actor.Props
import net.shift.common.DefaultLog
import net.shop.api.Formatter
import net.shift.common.Config
import akka.actor.Actor
import akka.actor.ActorSystem
import net.shift.common.Log
import net.shop.model.Formatters._
import net.shift.loc.Loc
import net.shop.web.ShopApplication
import net.shift.io.IODefaults
import java.util.Date
import net.shop.web.services.ServiceDependencies

sealed trait Message
case class OrderDocument(l: Language, o: Order, doc: String) extends Message
case class ForgotPassword(l: Language, email: String, doc: String) extends Message

sealed trait ActorMessage
case class StoreOrderStats(content: OrderDocument) extends ActorMessage
case class Mail(
  from: String,
  to: Seq[String],
  cc: Seq[String] = Seq.empty,
  bcc: Seq[String] = Seq.empty,
  subject: String,
  message: String) extends ActorMessage

object Messaging extends IODefaults {
  val orderActor = ActorSystem("idid").actorOf(Props[OrderActor], "orderActor")

  def send(m: Message)(implicit cfg : Config) {
    m match {

      case od @ OrderDocument(l, o, doc) =>
        orderActor ! StoreOrderStats(od)
        orderActor ! Mail(
          from = cfg.string("smtp.from"),
          to = List(o.email),
          bcc = cfg.list("smtp.bcc"),
          subject = Loc.loc0(l)("subject").text,
          message = doc)

      case ForgotPassword(l, email, doc) =>
        orderActor ! Mail(
          from = cfg.string("smtp.from"),
          to = List(email),
          subject = Loc.loc0(l)("recover.password").text,
          message = doc)

      case _ =>
    }
  }
}

trait OrderActor extends Actor with DefaultLog with ServiceDependencies {

  def receive = {
    case StoreOrderStats(content) => writeStat(content.o)("ro")
    case mail: Mail               => sendMail(mail)
  }

  def writeStat(o: Order)(implicit lang: String) =
    store.createOrder(o.toOrderLog)

  def sendMail(mail: Mail) {
    import org.apache.commons.mail._

    val commonsMail: Email = new HtmlEmail().setHtmlMsg(mail.message)
    mail.to foreach (commonsMail.addTo(_))
    mail.cc foreach (commonsMail.addCc(_))
    mail.bcc foreach (commonsMail.addBcc(_))

    commonsMail.setHostName(cfg.string("smtp.server"))
    commonsMail.setSmtpPort(cfg.int("smtp.port"))
    commonsMail.setAuthenticator(new DefaultAuthenticator(cfg.string("smtp.user"), cfg.string("smtp.password")))
    commonsMail.setSSLOnConnect(true);
    commonsMail.setFrom(mail.from).
      setSubject(mail.subject).
      send()
  }
}

