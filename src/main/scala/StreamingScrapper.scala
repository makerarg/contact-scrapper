import java.io.OutputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import io.circe.Decoder
import io.circe.generic.auto._
import model._
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}
import scalacache.modes.try_._

import scala.language.postfixOps
import scala.util.{Failure, Success}

class StreamingScrapper(cache: CaffeineCache, repo: ContactRepo)(implicit actorSystem: ActorSystem) {

  /** Make a Source that will parse [[ByteString]]s and materialize as an [[OutputStream]]  */
  def parsingFlow[R <: RawContact](source: Source[ByteString, _])(implicit decoder: Decoder[R]): Source[Contact, _] = {
    source
      .via(Flow.fromFunction[ByteString, ByteString]( bs => {
        println(s"Getting chunks ${bs}")
        bs
      }))
      .via(CirceStreamSupport.decode[R](AsyncParser.UnwrapArray))
      .map(Contact(_))
  }

  /** Make a streamed GET request to the given url and return the source as a Readable object  */
  def requestStreamed(url: String): geny.Readable =
    requests
      .get
      .stream(url, onHeadersReceived = { sh => println(s"SH arrived: $sh")})

  val source: Source[Coordinates, NotUsed] = LocationReader.coordinateSource
  val coordinatesToRequestInfoFlow: Flow[Coordinates, List[RequestInfo[_]], NotUsed] =
    Flow.fromFunction[Coordinates, List[RequestInfo[_]]]{ coordinates =>
      println(s"$coordinates")
      List(
        RequestInfo[OrmiFlexContact](OrmiFlex, coordinates),
        RequestInfo[MegaFlexContact](MegaFlex, coordinates)
      )
    }
  val infoToContactSource: RequestInfo[_] => Source[Contact, _] = { info: RequestInfo[_] =>
    val url = info.source.url(info.coordinates)
    println(s"making request to $url")

    lazy val source = requestStreamed(url).readBytesThrough(inputStream => {
      StreamConverters.fromInputStream(() => inputStream)
    })
    info.source.id match {
      case OrmiFlex.id => parsingFlow[OrmiFlexContact](source)
      case MegaFlex.id => parsingFlow[MegaFlexContact](source)
    }
  }
  val cacheContactFlow: Flow[Contact, Option[String], NotUsed] =
    Flow.fromFunction[Contact, Option[String]](contact => {
      cache.contactCache.put(contact.id)(contact) match {
        case Success(_) => Some(contact.id)
        case Failure(ex) =>
          println(s"contactCache.put failed with ex ${ex.getMessage}")
          None
      }
    })

  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
  val graph: RunnableGraph[NotUsed] = source
    .via(coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .flatMapConcat(infoToContactSource)
    .via(cacheContactFlow)
    .to(Sink.foreach {
      case Some(id) =>
        cache.contactCache.get(id) match {
          case Success(contact) =>
            contact.map(repo.safeInsertContact(_).unsafeRunAsync {
              case Right(_) => ()
              case Left(ex) =>
                println(s"DB write failed for ${id}")
                println(ex.getMessage)
                ()
            })
          case Failure(ex) =>
            println(s"Cache retrieval failed for ${id}")
            println(ex.getMessage)
            ()
        }
      case _ => ()
    })
}
