import akka.actor.ActorSystem

object LocationReaderSpec extends App {
  import LocationReader._

  implicit val actorSystem = ActorSystem("LocationSpec")

  locationGraph.run()
}
