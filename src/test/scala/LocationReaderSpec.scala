import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import model.Coordinates
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.util.{Failure, Success}

object LocationReaderSpec extends App {
  import LocationReader._

  implicit val actorSystem = ActorSystem("LocationSpec")
  val testCache: Cache[Coordinates] = CaffeineCache[Coordinates]

  /** Iterator results: SUCCESS */
  /*
  var caba = 0
  while(CABAReaderIterator.hasNext) {
    println(s"CABAResult ${caba}")
    println(CABAReaderIterator.next())
    caba +=1
  }

  var pba = 0
  while(PBAReaderIterator.hasNext) {
    println(s"PBAResult ${pba}")
    println(PBAReaderIterator.next())
    pba +=1
  }
   */

  /** Source results: SUCCESS */
  /*
  CABASource.to(Sink.foreach(println)).run()
  PBASource.to(Sink.foreach(println)).run()
   */

  var n = 0
  coordinateSource
    .via(Flow.fromFunction[Coordinates, Coordinates]{ c =>
      import scalacache.modes.try_._
      val key = UUID.randomUUID().toString
      testCache.put(key)(c) match {
        case Success(_) =>
          n += 1
          println(n)
          println(s"successfully stored $c")
          println(s"${testCache.get(key)}")
          c
        case Failure(exception) =>
          println(s"$exception")
          c
      }
    })
    .to(Sink.foreach(println(_)))
    .run()
}
