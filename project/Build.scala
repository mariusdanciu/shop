import sbt._
import Keys._

object ShopBuild extends Build {

  lazy val root = Project(id = "shop",
                          base = file(".")) aggregate(shopweb, shopdatabase)

  lazy val shopweb = Project(id = "shopweb",
				  base = file("shopweb"))

  lazy val shopdatabase = Project(id = "shopdatabase",
				  base = file("shopdatabase"))

}
