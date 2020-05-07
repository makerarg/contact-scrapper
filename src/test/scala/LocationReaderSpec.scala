import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink

object LocationReaderSpec extends App {
  import LocationReader._

  implicit val actorSystem = ActorSystem("LocationSpec")

  coordinateSource
    .to(Sink.foreach(println(_)))
    .run()
}
