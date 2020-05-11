import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import cats.effect.{ContextShift, IO}
import db.DBConfig
import model._
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ScrapperApp extends App {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new DBConfig)

  val streamingScrapper = new StreamingScrapper(cache, repo)

  streamingScrapper.stream.run()

}

case class RequestInfo[R <: RawContact](source: ContactSource[R], coordinates: Coordinates)
