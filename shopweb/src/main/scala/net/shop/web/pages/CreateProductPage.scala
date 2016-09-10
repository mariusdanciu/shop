package net.shop.web.pages

import net.shift.http.HTTPRequest
import scala.xml.NodeSeq
import net.shop.web.services.ServiceDependencies
import scala.util.Success
import net.shift.common.XmlAttr
import scala.xml.Text
import scala.xml._
import net.shift.common.XmlUtils._
import net.shift.common.Xml
import net.shift.common.XmlImplicits._

trait CreateProductPage extends Cart[HTTPRequest] with ServiceDependencies { self =>
  override def snippets = List(catList) ++ super.snippets

  val catList = reqSnip("catlist") {
    s =>
      store.allCategories match {
        case Success(cats) =>
          val res = NodeSeq.fromSeq((for { c <- cats } yield {
            Xml("option", XmlAttr("value", c.stringId)) / Text(c.title_?(s.state.initialState.language.name))
          }).toSeq)
          Success((s.state.initialState, res))
        case _ => Success((s.state.initialState, NodeSeq.Empty))
      }
  }
}
