import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import eu.timepit.refined.string.Url
import kantan.csv.ReadResult
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

object ScrapperApp extends App {

  implicit val actorSystem = ActorSystem("ScrapSys")
  val streamingScrapper = new StreamingScrapper

  import io.circe.generic.auto._

  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
  LocationReader.locationSource
    .via(LocationReader.parserFlow)
    .mapConcat(c => {
      println(s"$c")
      Seq(
        RequestInfo[OrmiFlexContact](OrmiFlex, c),
        RequestInfo[MegaFlexContact](MegaFlex, c))
    })
    .map(info => {
      val url = info.source.url(info.coordinates)
      println(s"making request to $url")
      (streamingScrapper.requestStreamed(url), info.source.id)
    })
    .flatMapConcat {
      case (readable, id) =>
        val src = readable.readBytesThrough[Source[ByteString, _]](is => {
          StreamConverters.fromInputStream(() => is)
        })
        id match {
          case OrmiFlex.id => streamingScrapper.parsingStream[OrmiFlexContact](src)
          case MegaFlex.id => streamingScrapper.parsingStream[MegaFlexContact](src)
        }
    }
    .to(Sink.foreach(println))
    .run()

}

case class RequestInfo[R <: RawContact](source: ContactSource[R], coordinates: Coordinates)
