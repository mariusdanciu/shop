import sbt._
import Keys._
import io._

object ShopBuild extends Build {

  val distDir = new File("./dist")
  val libDir = distDir / "lib"
  val scriptsDir = new File("./scripts")

  val distShopWeb = TaskKey[File]("distShopWeb", "")
  val distShopDB = TaskKey[File]("distShopDB", "")
  val dist = TaskKey[Unit]("dist", "")
  
  val distShopWebSetting = distShopWeb <<= (target, managedClasspath in Runtime, packageBin in Compile) map {
    (target, cp, pack) =>
	    println("dist > shopweb")
	    
	    IO.delete(distDir)
	    IO.createDirectory(distDir)
	    IO.createDirectory(libDir)
	    
	    for {jar <- cp} {
	      IO.copyFile(jar.data, libDir / jar.data.name);
	    }
	    
	    IO.copyDirectory ( new File("shopweb") / "web", distDir / "web");
	    IO.copyDirectory ( new File("shopweb") / "config", distDir / "config");
	    IO.copyDirectory ( new File("shopweb") / "data", distDir / "data");
	    IO.copyDirectory ( new File("shopweb") / "localization", distDir / "localization");
	    
	    pack
  }
  
  val distShopDBSetting = distShopDB <<= (target, managedClasspath in Runtime, packageBin in Compile) map {
    (target, cp, pack) => {
        println("dist > shopdatabase")
	    pack
    }
  }
  
  val distSetting = dist <<= (target, distShopWeb in shopweb, distShopDB in shopdatabase) map { (target, f, fdb) => {
      println("dist > shop")
      IO.copyFile(f, libDir / f.name);
      IO.copyFile(fdb, libDir / fdb.name);
      IO.copyFile(scriptsDir / "start.sh", distDir / "start.sh");
    }
  }
  
  lazy val root = Project(id = "shop", 
		  				  base = file("."),
		  				  settings = Defaults.defaultSettings ++ Seq(distSetting, 
		  				      distShopWebSetting, 
		  				      distShopDBSetting)) aggregate (shopweb, shopdatabase)

  lazy val shopweb = Project(id = "shopweb", 
      base = file("shopweb"), 
      settings = Defaults.defaultSettings ++ Seq(distShopWebSetting))

  lazy val shopdatabase = Project(id = "shopdatabase", 
      base = file("shopdatabase"),
      settings = Defaults.defaultSettings ++ Seq(distShopDBSetting))

}
