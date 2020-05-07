import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import model._
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ScrapperApp extends App {

  implicit val actorSystem = ActorSystem("ScrapSys")
  val streamingScrapper = new StreamingScrapper
  val cache = new CaffeineCache

  import io.circe.generic.auto._

  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
  LocationReader.coordinateSource
    .mapConcat[RequestInfo[_]](coordinates => {
      println(s"$coordinates")
      Seq(
        RequestInfo[OrmiFlexContact](OrmiFlex, coordinates),
        RequestInfo[MegaFlexContact](MegaFlex, coordinates)
      )
    })
    .map[(geny.Readable, String)](info => {
      val url = info.source.url(info.coordinates)
      println(s"making request to $url")
      (streamingScrapper.requestStreamed(url), info.source.id)
    })
    .flatMapConcat {
      case (readable, id) =>
        val src0: Source[ByteString, _] = readable.readBytesThrough[Source[ByteString, _]](is => {
          StreamConverters.fromInputStream(() => is)
        })
        id match {
          case OrmiFlex.id => streamingScrapper.parsingStream[OrmiFlexContact](src0)
          case MegaFlex.id => streamingScrapper.parsingStream[MegaFlexContact](src0)
        }
    }
    .via(Flow.fromFunction[Contact[_], Unit](contact => {
      import scalacache.modes.try_._

      cache.caffeineCache.put(contact.id)(contact)
    }))
    .to(Sink.foreach(println))
    .run()

}

case class RequestInfo[R <: RawContact](source: ContactSource[R], coordinates: Coordinates)
