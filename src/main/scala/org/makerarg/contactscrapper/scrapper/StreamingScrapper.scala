package org.makerarg.contactscrapper.scrapper

import java.io.OutputStream

import akka.NotUsed
import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.CompletionStrategy
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
  def parseStream[R <: RawContact](source: Source[ByteString, _])(implicit decoder: Decoder[R]): Source[Contact, _] = {
    source
      .log("Parsing chunks")
      .via(CirceStreamSupport.decode[R](AsyncParser.UnwrapArray))
      .map(Contact(_))
  }

  /** Make a streamed GET request to the given url and return the source as a Readable object  */
  private val streamedRequest: String => geny.Readable = { url =>
    requests
      .get
      .stream(url)
  }

  private val requestToStream: RequestInfo => Source[ByteString, _] = { info =>
    val url = info.source.url(info.coordinates)
    println(s"Making streamed request to $url")

    streamedRequest(url).readBytesThrough(inputStream => {
      StreamConverters.fromInputStream(() => inputStream)
    })
  }

  val infoToContactSource: RequestInfo => Source[Contact, _] = { info =>
    info.source match {
      case OrmiFlex => parseStream[OrmiFlexContact](requestToStream(info))
      case MegaFlex => parseStream[MegaFlexContact](requestToStream(info))
    }
  }

  val cacheContact: Contact => Option[ContactId] = { contact =>
    cache.contactCache.put(contact.id)(contact) match {
      case Success(_) =>
        println(s"Successful cache write. id: ${contact.id}")
        //TODO: Send message to source actor
        Some(contact.id)
      case Failure(ex) => println(s"Failed cache write. ${ex.getMessage}"); None
    }
  }

  val writeToDBFromCache: ContactId => Unit = { id =>
    cache.contactCache.get(id) match {
      case Success(_) => writeToDBAsync
      case Failure(ex) => println(s"Failed cache retrieve. ${id} - ${ex.getMessage}")
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
    .mapConcat(coordinatesToRequestInfo)
    .groupBy(
      maxSubstreams = 2,
      _.source.id,
      allowClosedSubstreamRecreation = true)
    .async
    .flatMapConcat(infoToContactSource)
    .via(Flow.fromFunction[Contact, Option[ContactId]](cacheContact))
    .to(Sink.ignore)

  /**
   * TODO: Set up an actor source that receives `ContactId`s, looks them up in the cache and writes to the DB.
   */
  val writeSource: Source[Any, ActorRef] = Source.actorRef(
    completionMatcher = {
      case Done =>
        // complete stream immediately if we send it Done
        CompletionStrategy.immediately
    },
    // never fail the stream because of a message
    failureMatcher = PartialFunction.empty,
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.dropHead)
  val readSideActor: ActorRef = writeSource.to(Sink.foreach(println)).run()

}
