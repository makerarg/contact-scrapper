import akka.actor.ActorSystem
import cache.CaffeineCache
import cats.effect.{ContextShift, IO}
import db.DBConfig
import model._
import thirdparties.RawContact

import scala.concurrent.ExecutionContext

object ScrapperApp extends App {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new DBConfig)

  val streamingScrapper = new StreamingScrapper(cache, repo)

  streamingScrapper.graph.run()

}

case class RequestInfo[R <: RawContact](source: ContactSource[R], coordinates: Coordinates)
