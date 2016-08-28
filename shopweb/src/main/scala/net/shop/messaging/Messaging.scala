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
import net.shop.api.persistence.Persistence
import net.shift.io.IODefaults
import net.shift.io.LocalFileSystem

sealed trait Message
case class OrderDocument(l: Language, o: Order, doc: String) extends Message
case class ForgotPassword(l: Language, email: String, doc: String) extends Message

sealed trait ActorMessage
case class StoreOrderStats(content: OrderDocument, store: Persistence) extends ActorMessage
case class Mail(
  from: String,
  to: Seq[String],
  cc: Seq[String] = Seq.empty,
  bcc: Seq[String] = Seq.empty,
  subject: String,
  message: String,
  cfg: Config) extends ActorMessage

object Messaging {
  val orderActor = ActorSystem("idid").actorOf(Props[OrderActor], "orderActor")

  implicit val fs = LocalFileSystem

  def send(m: Message)(implicit conf: Config, store: Persistence) {
    m match {

      case od @ OrderDocument(l, o, doc) =>
        orderActor ! StoreOrderStats(od, store)
        orderActor ! Mail(
          from = conf.string("smtp.from"),
          to = List(o.email),
          bcc = conf.list("smtp.bcc"),
          subject = Loc.loc0(l)("subject").text,
          message = doc,
          cfg = conf)

      case ForgotPassword(l, email, doc) =>
        orderActor ! Mail(
          from = conf.string("smtp.from"),
          to = List(email),
          subject = Loc.loc0(l)("recover.password").text,
          message = doc,
          cfg = conf)

      case _ =>
    }
  }
}

class OrderActor extends Actor with DefaultLog {

  def receive = {
    case StoreOrderStats(content, store) => writeStat(content.o, store)("ro")
    case mail: Mail                      => sendMail(mail)
  }

  def writeStat(o: Order, store: Persistence)(implicit lang: String) =
    store.createOrder(o.toOrderLog)

  def sendMail(mail: Mail) {
    import org.apache.commons.mail._

    val commonsMail: Email = new HtmlEmail().setHtmlMsg(mail.message)
    mail.to foreach (commonsMail.addTo(_))
    mail.cc foreach (commonsMail.addCc(_))
    mail.bcc foreach (commonsMail.addBcc(_))

    commonsMail.setHostName(mail.cfg.string("smtp.server"))
    commonsMail.setSmtpPort(mail.cfg.int("smtp.port"))
    commonsMail.setAuthenticator(new DefaultAuthenticator(mail.cfg.string("smtp.user"), mail.cfg.string("smtp.password")))
    commonsMail.setSSLOnConnect(true);
    commonsMail.setFrom(mail.from).
      setSubject(mail.subject).
      send()

    log.info("Sent order to: " + mail.to)
  }
}

