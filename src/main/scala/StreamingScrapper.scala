import java.io.OutputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import geny.Bytes
import io.circe.Decoder
import io.circe.generic.auto._
import model._
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}
import scalacache.modes.try_._
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.reflect.ClassTag
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

  /** Make a GET request to the given url and return the source as a Readable object  */
  def request[R <: RawContact: ClassTag](url: String)(implicit decoder: Decoder[R]): Seq[Contact] = {
    import io.circe.parser.decode

    println(s"Making request to ${url}")
    val res = requests.get(url).text
    decode[Seq[R]](res) match {
      case Left(err) => println(err); Seq.empty
      case Right(contacts) => contacts.map(Contact(_))
    }
  }

  val source: Source[Coordinates, NotUsed] = LocationReader.coordinateSource
  def coordinatesToRequestInfoFlow[R <: RawContact]: Flow[Coordinates, List[RequestInfo], NotUsed] = {
    Flow.fromFunction[Coordinates, List[RequestInfo]]{ coordinates =>
      println(s"$coordinates")
      List(
        RequestInfo(OrmiFlex, coordinates),
        RequestInfo(MegaFlex, coordinates)
      )
    }
  }
  val requestToStream: RequestInfo => Source[ByteString, _] = { info =>
    val url = info.source.url(info.coordinates)
    println(s"making streamed request to $url")

    requestStreamed(url).readBytesThrough(inputStream => {
      StreamConverters.fromInputStream(() => inputStream)
    })
  }
  val ormiInfoToContactSource: RequestInfo => Source[Contact, _] = { info =>
    parsingFlow[OrmiFlexContact](requestToStream(info))
  }
  val megaInfoToContactSource: RequestInfo => Source[Contact, _] = { info =>
    parsingFlow[MegaFlexContact](requestToStream(info))
  }
  def infoToContacts[R <: RawContact: ClassTag](info: RequestInfo)(implicit decoder: Decoder[R]): Seq[Contact] = {
    request[R](info.source.url(info.coordinates))
  }

  val cacheContactFlow: Flow[Contact, Option[String], NotUsed] = {
    Flow.fromFunction[Contact, Option[String]](contact => {
      cache.contactCache.put(contact.id)(contact) match {
        case Success(_) =>
          println(s"Successful cache write with id: ${contact.id}")
          Some(contact.id)
        case Failure(ex) =>
          println(s"contactCache.put failed with ex ${ex.getMessage}")
          None
      }
    })
  }
  val writeToDBFromCache: Option[String] => Unit = {
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
  }
  val writeToDB: Contact => Unit = { contact =>
    repo.safeInsertContact(contact).unsafeRunAsync {
      case Right(_) =>
        println(s"Successful DB Insert ${contact.id}")
        ()
      case Left(ex) =>
        println(s"DB write failed for ${contact.id}")
        println(ex.getMessage)
        ()
    }
  }


  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
    /*
  val graph: RunnableGraph[NotUsed] = source
    .via(coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .flatMapConcat(infoToContactSource)
    .via(cacheContactFlow)
    .to(Sink.foreach(writeToDBFromCache))

     */

  /**
   * New Flow:
   *  - Read [[Coordinate]]s
   *  - Build 2 sources for each coordinate (Ormi and Mega)
   *  - Make requests in parallel on each Stream
   *  - Parse as [[Contact]]
   *  - Merge into single Sink that writes to the DB
   */
  val graph2: RunnableGraph[NotUsed] = source
    .via(coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .groupBy(
      maxSubstreams = 2,
      _.source.id,
      allowClosedSubstreamRecreation = true)
    .async
//    .flatMapConcat({
//      case info: RequestInfo =>
//        infoToContacts(info)
//    })
//    .via(cacheContactFlow)
//    .mergeSubstreams.async
//    .to(Sink.foreach(writeToDBFromCache))
    .to(Sink.ignore)

  val graph3: RunnableGraph[NotUsed] = source
    .via(coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .groupBy(
      maxSubstreams = 2,
      _.source.id,
      allowClosedSubstreamRecreation = true)
    .mapConcat({ info =>
      import RawContact.decodeEvent
      val contacts = infoToContacts(info)
      println(contacts)
      contacts
    })
//    .via(cacheContactFlow)
//    .to(Sink.foreach(writeToDBFromCache))
    //TODO: The database is timing out (too many connections)!!!!!!!
    .to(Sink.foreach(writeToDB))
//    .mergeSubstreams

}
