package net.shop
package web

import net.shift._
import engine.ShiftApplication
import engine.http._
import engine.page._
import HttpPredicates._
import netty.NettyServer
import ShiftApplication._
import template._
import Snippet._

import pages._

object StartShop extends App {
  import ShopUtils._
  
  println("Starting shop application ...");
  
  NettyServer.start(8080, new ShiftApplication {

    def servingRule = 
      css |
      js | 
      page("/", "web/index.html", Index) |
      service(notFoundService)
  })
}

object ShopUtils {
  
  def notFoundService(resp: AsyncResponse) {
    resp(TextResponse("Sorry ... service not found"))
  }
  
  def page(uri: String, filePath: String, snipets: DynamicContent[Request]) = for {
      _ <- path(uri)
    } yield Html5(filePath, Index)

  def css = for {
      "styles" :: file :: _ <- path
    } yield service(resp => resp(CSSResponse(scala.io.Source.fromFile("web/styles/"+file).mkString)))

  def js = for {
      "scripts" :: file :: _ <- path
    } yield service(resp => resp(JSResponse(scala.io.Source.fromFile("web/scripts/"+file).mkString)))
  
}

