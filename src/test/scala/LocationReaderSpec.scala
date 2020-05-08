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

  coordinateSource
    .via(Flow.fromFunction[Coordinates, Coordinates]{ c =>
      import scalacache.modes.try_._
      val key = UUID.randomUUID().toString
      testCache.put(key)(c) match {
        case Success(_) =>
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
