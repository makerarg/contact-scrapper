import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import eu.timepit.refined.string.Url
import kantan.csv.ReadResult
import thirdparties.{MegaFlexContact, OrmiFlexContact}

object ScrapperApp extends App {

  implicit val actorSystem = ActorSystem("ScrapSys")
  val streamingScrapper = new StreamingScrapper

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
    .via(Flow.fromFunction[RequestInfo[_], geny.Readable](info => {
      val url = info.source.url(info.coordinates)
      println(s"making request to $url")
      streamingScrapper.requestStreamed(url)
    }))
    .to(Sink.foreach(println))
    .run()
    /*
    .map(readable =>
      readable.writeBytesTo(
        streamingScrapper
          .parsingStream
          .to(Sink.ignore)
          .run
      )
    )
    */

}

case class RequestInfo[R](source: ContactSource[R], coordinates: Coordinates)
