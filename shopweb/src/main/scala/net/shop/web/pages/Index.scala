package net.shop
package web.pages

import scala.xml._
import net.shift._
import template._
import engine.http._
import Snippet._

object Index extends DynamicContent[Request] {

  def snippets = List()
 
  def reqSnip(name: String) = snip[Request](name) _
  
  val elem1 = reqSnip("elem1"){
    s => (s.state, <p>Elem1</p>)
  }
  
}