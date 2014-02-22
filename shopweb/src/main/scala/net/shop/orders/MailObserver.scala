package net.shop
package orders

import backend._
import net.shop.model.Order
import net.shift.common.Config
import net.shift.loc.Loc

object MailObserver extends OrderObserver {
  implicit def stringToSeq(single: String): Seq[String] = Seq(single)

  def onOrder(content: OrderDocument) {
    send a Mail(
      from = ("vanzari@handmade.ro", ""),
      to = "marius.danciu@gmail.com",
      bcc = "aleena.danciu@gmail.com",
      subject = Loc.loc0(content.l)("order.subject").text,
      message = content.doc)
  }
}

case class Mail(
  from: (String, String),
  to: Seq[String],
  cc: Seq[String] = Seq.empty,
  bcc: Seq[String] = Seq.empty,
  subject: String,
  message: String)

object send {
  def a(mail: Mail) {
    import org.apache.commons.mail._

    val commonsMail: Email = new HtmlEmail().setHtmlMsg(mail.message)
    mail.to foreach (commonsMail.addTo(_))
    mail.cc foreach (commonsMail.addCc(_))
    mail.bcc foreach (commonsMail.addBcc(_))

    commonsMail.setHostName(Config.string("smtp.server"))
    commonsMail.setSmtpPort(Config.int("smtp.port"))
    commonsMail.setAuthenticator(new DefaultAuthenticator(Config.string("smtp.user"), Config.string("smtp.password")))
    commonsMail.setSSLOnConnect(true);
    commonsMail.setFrom(mail.from._1, mail.from._2).
      setSubject(mail.subject).
      send()
  }
}