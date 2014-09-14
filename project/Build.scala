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
  
  val distShopDBSetting = distShopDB <<= (target, managedClasspath in Runtime, publishLocal, packageBin in Compile) map {
    (target, cp, _, pack) => {
        println("dist > shopdatabase")
	    pack
    }
  }
  
  val distSetting = dist <<= (target, scalaVersion, version, distShopWeb in shopweb, distShopDB in shopdatabase) map {
    (target, sv, v, f, fdb) => {
      println("dist > shop")
      IO.copyFile(f, libDir / f.name);
      IO.copyFile(fdb, libDir / fdb.name);
      IO.copyFile(scriptsDir / "start.sh", distDir / "start.sh");
      TarGzBuilder.makeTarGZ("target/idid_" + sv + "_" + v + "_.tar.gz")
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
      settings = Defaults.defaultSettings ++ Seq(distShopDBSetting)) dependsOn (shopweb)

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
