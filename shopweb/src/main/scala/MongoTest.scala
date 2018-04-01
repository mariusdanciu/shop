import net.shop.persistence.mongodb.MongoPersistence
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MongoTest extends App {

  println("Here")

  System.setProperty("org.mongodb.async.type", "netty")
  val uri = "mongodb://idid:qwer1234@idid-shard-00-00-ksqgy.mongodb.net:27017,idid-shard-00-01-ksqgy.mongodb.net:27017,idid-shard-00-02-ksqgy.mongodb.net:27017/?ssl=true&replicaSet=idid-shard-0&authSource=admin"

  val p = MongoPersistence(uri)

  //p.allCategories.map(c => c.seq.map { s => println(s) })


  p.categoryProducts("ceasuri") match {
    case Success(s) => println (s.mkString("\n"))
    case Failure(t) => t.printStackTrace
  }
  // p.categoryProducts("corpuri de iluminat", SortByName(true, "ro")).map(println)

  // p.searchProducts("spalare", SortByName(true, "ro")).map(println)

  Console.in.readLine()


}