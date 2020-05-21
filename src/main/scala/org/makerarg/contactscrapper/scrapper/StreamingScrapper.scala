package org.makerarg.contactscrapper.scrapper

import java.io.OutputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, StreamConverters}
import akka.util.ByteString
import io.circe.Decoder
import io.circe.generic.auto._
import org.makerarg.contactscrapper.ContactId
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.ContactRepo
import org.makerarg.contactscrapper.model._
import org.makerarg.contactscrapper.thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import scalacache.modes.try_._

import scala.language.postfixOps
import scala.util.{Failure, Success}

class StreamingScrapper(
  val cache: CaffeineCache,
  val repo: ContactRepo
)(implicit val actorSystem: ActorSystem) extends Scrapper {

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
  def requestStreamed(url: String): geny.Readable = {
    requests
      .get
      .stream(url, onHeadersReceived = { sh => println(s"SH arrived: $sh")})
  }

  val requestToStream: RequestInfo => Source[ByteString, _] = { info =>
    val url = info.source.url(info.coordinates)
    println(s"making streamed request to $url")

    requestStreamed(url).readBytesThrough(inputStream => {
      StreamConverters.fromInputStream(() => inputStream)
    })
  }

  val infoToContactSource: RequestInfo => Source[Contact, _] = { info =>
    info.source match {
      case OrmiFlex => parsingFlow[OrmiFlexContact](requestToStream(info))
      case MegaFlex => parsingFlow[MegaFlexContact](requestToStream(info))
    }
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
  val writeToDBFromCache: ContactId => Unit = { id =>
    cache.contactCache.get(id) match {
      case Success(contact) =>
        contact.foreach(repo.safeInsertContact(_).unsafeRunAsyncAndForget())
        ()
      case Failure(ex) =>
        println(s"Cache retrieval failed for ${id}")
        println(ex.getMessage)
        ()
    }
  }

  /**
   * Flow:
   *  - Read [[Coordinate]]s
   *  - Group into subsources for each coordinate (Ormi and Mega)
   *  - Make requests in parallel on each Stream
   *  - Parse as [[Contact]]
   *  - Merge into single Sink that writes to the DB
   */
  val graph: RunnableGraph[NotUsed] = source
    .via(coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .groupBy(
      maxSubstreams = 2,
      _.source.id,
      allowClosedSubstreamRecreation = true)
    .async
    .flatMapConcat(infoToContactSource)
    .via(cacheContactFlow)
//    .mergeSubstreams.async
//    .to(Sink.foreach(writeToDBFromCache))
    .to(Sink.foreach(println(_)))

  val writeSource: Source[ContactId, NotUsed] = Source.empty[ContactId]
  val readSideGraph: RunnableGraph[NotUsed] = Source.empty.to(Sink.ignore)

}
