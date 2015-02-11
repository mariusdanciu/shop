import sbt._
import Keys._
import io._

object ShopBuild extends Build {

  val distDir = new File("./dist")
  val libDir = distDir / "lib"
  val scriptsDir = new File("./scripts")

  val distShopApi = TaskKey[File]("distShopApi", "")
  val distShopWeb = TaskKey[File]("distShopWeb", "")
  val distShopDB = TaskKey[File]("distShopDB", "")
  val dist = TaskKey[Unit]("dist", "")

  val buildProps = {
    import java.util.Properties
 
    val prop = new Properties()
    val file = new File("./build.properties")
    
    IO.load(prop, file)
    prop.setProperty("build", prop.getProperty("build").toInt + 1 + "");
    IO.write(prop, "", file)
    prop
  }
  
  val distShopWebSetting = distShopWeb <<= (target, managedClasspath in Runtime, publishLocal, packageBin in Compile) map {
    (target, cp, _, pack) =>
	    println("dist > shopweb")
	    
	    IO.delete(distDir)
	    IO.createDirectory(distDir)
	    IO.createDirectory(libDir)
	    
	    for {jar <- cp} {
	      IO.copyFile(jar.data, libDir / jar.data.name);
	    }
	    
	    val showWebDir =  new File("shopweb") 
	    
	    IO.copyDirectory ( showWebDir / "web", distDir / "web");
	    IO.copyDirectory ( showWebDir / "config", distDir / "config");
	    IO.copyDirectory ( showWebDir / "data", distDir / "data");
	    IO.copyDirectory ( showWebDir / "localization", distDir / "localization");
	    
	    pack
  }

  val distShopApiSetting = distShopApi <<= (target, managedClasspath in Runtime, publishLocal, packageBin in Compile) map {
    (target, cp, _, pack) => {
        println("dist > shopapi")
	pack
    }
  }
  
  val distShopDBSetting = distShopDB <<= (target, managedClasspath in Runtime, publishLocal, packageBin in Compile) map {
    (target, cp, _, pack) => {
        println("dist > shopdatabase")
	pack
    }
  }
  
  val distSetting = dist <<= (target, scalaVersion, version, distShopApi in shopapi, distShopWeb in shopweb, distShopDB in shopdatabase) map {
    (target, sv, v, api, web, db) => {
      println("dist > shop")
      IO.copyFile(api, libDir / api.name);
      IO.copyFile(web, libDir / web.name);
      IO.copyFile(db, libDir / db.name);
      IO.copyFile(scriptsDir / "start.sh", distDir / "start.sh");
      TarGzBuilder.makeTarGZ("target/idid_" + sv + "_" + v + "_.tar.gz")
    }
  }
  
  
  lazy val root = Project(id = "shop", 
      base = file("."),
      settings = Defaults.defaultSettings ++ Seq(distSetting, 
         distShopApiSetting,
         distShopWebSetting, 
         distShopDBSetting)) aggregate (shopapi, shopweb, shopdatabase)

  lazy val shopapi = Project(id = "shopapi", 
      base = file("shopapi"), 
      settings = Defaults.defaultSettings ++ Seq(distShopApiSetting))


  lazy val shopdatabase = Project(id = "shopdatabase", 
      base = file("shopdatabase"),
      settings = Defaults.defaultSettings ++ Seq(distShopDBSetting)) dependsOn (shopapi)

  lazy val shopweb = Project(id = "shopweb", 
      base = file("shopweb"), 
      settings = Defaults.defaultSettings ++ Seq(distShopWebSetting)) dependsOn (shopapi, shopdatabase)

}


object TarGzBuilder {
  
  import java.io._
  import org.apache.commons.compress.archivers.tar._
  import org.apache.commons.compress.compressors.gzip._
  
  
  def makeTarGZ(name: String) {
     val tOut = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(new File(name)))))
     try {
       populateTarGz(tOut, "./dist")
     } finally {
       tOut.close();
     } 
  }
  
  
  def populateTarGz(tOut: TarArchiveOutputStream, path: String, base: String = null) {
    val f = new File(path);
    val entryName = if (base == null) "idid" else (base + f.getName());
    val tarEntry = new TarArchiveEntry(f, entryName);
    tOut.putArchiveEntry(tarEntry);

    if (f.isFile()) {
      IO.transfer(f, tOut);
      tOut.closeArchiveEntry();
    } else {
      tOut.closeArchiveEntry();
      val children = f.listFiles();
      if (children != null){
        for (child <- children) {
          populateTarGz(tOut, child.getAbsolutePath(), entryName + "/");
        }
      }
    }
  }
  
}
