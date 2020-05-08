import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import db.DBConfig
import model._
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

import scala.util.{Failure, Success}

object ScrapperApp extends App {

  implicit val actorSystem = ActorSystem("ScrapSys")
  val streamingScrapper = new StreamingScrapper
  val cache = new CaffeineCache
  val dbConfig = new DBConfig

  import io.circe.generic.auto._
  import scalacache.modes.try_._

  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
  val parsingStream = LocationReader.coordinateSource
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
        val src0: Source[ByteString, _] = readable.readBytesThrough[Source[ByteString, _]](inputStream => {
          StreamConverters.fromInputStream(() => inputStream)
        })
        id match {
          case OrmiFlex.id => streamingScrapper.parsingStream[OrmiFlexContact](src0)
          case MegaFlex.id => streamingScrapper.parsingStream[MegaFlexContact](src0)
        }
    }
    .via(Flow.fromFunction[Contact[_], Option[String]](contact => {
      cache.contactCache.put(contact.id)(contact) match {
        case Success(_) => Some(contact.id)
        case Failure(ex) =>
          println(s"contactCache.put failed with ex ${ex.getMessage}")
          None
      }
    }))
    .map {
      case Some(id) =>
        cache.contactCache.get(id)
      case _ => ()
    }
    .to(Sink.ignore)
    .run()

  cache.contactCache

}

case class RequestInfo[R <: RawContact](source: ContactSource[R], coordinates: Coordinates)
