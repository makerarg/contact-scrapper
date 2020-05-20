import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.makerarg.contactscrapper.model.Coordinates
import org.scalatest.{FreeSpec, Matchers}
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.util.{Failure, Success}

class LocationReaderSpec extends FreeSpec with Matchers {
  import org.makerarg.contactscrapper.LocationReader._

  implicit val actorSystem: ActorSystem = ActorSystem("LocationSpec")
  val testCache: Cache[Coordinates] = CaffeineCache[Coordinates]

  "coordinateSource + a cache" - {
    "should go through every location and cache it" in {
      var n = 0
      coordinateSource
        .via(Flow.fromFunction[Coordinates, Coordinates]{ c =>
          import scalacache.modes.try_._
          testCache.put(UUID.randomUUID().toString)(c) match {
            case Success(_) => n += 1; c
            case Failure(exception) => println(s"$exception"); c
          }
        })
        .to(Sink.ignore)
        .run()

      Thread.sleep(1000)
      n shouldBe 1871
    }
  }
}
