package org.makerarg.contactscrapper.scrapper

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, StreamConverters}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.util.ByteString
import cats.data.EitherT
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

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class StreamingScrapper(
  val cache: CaffeineCache,
  val repo: ContactRepo
)(implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) extends Scrapper {

  /** Transform [[ByteString]] input into [[Contact]]  */
  def parseStream[R <: RawContact](source: Source[ByteString, _])(implicit decoder: Decoder[R]): Source[Contact, _] = {
    source
      .via(CirceStreamSupport.decode[R](AsyncParser.UnwrapArray))
      .map(Contact(_))
  }

  /** Make a streamed GET request to the given url and return the source as a Readable object  */
  private val streamedRequest: String => geny.Readable = { url =>
    requests
      .get
      .stream(url)
  }

  private val requestToByteSource: RequestInfo => Source[ByteString, _] = { info =>
    val url = info.source.url(info.coordinates)
    println(s"Making streamed request to $url")

    streamedRequest(url).readBytesThrough(inputStream => {
      StreamConverters.fromInputStream(() => inputStream)
    })
  }

  val infoToContactSource: RequestInfo => Source[Contact, _] = { info =>
    info.source match {
      case OrmiFlex => parseStream[OrmiFlexContact](requestToByteSource(info))
      case MegaFlex => parseStream[MegaFlexContact](requestToByteSource(info))
    }
  }

  val cacheContact: Contact => Option[ContactId] = { contact =>
    cache.contactCache.get(contact.id) match {
      case Success(Some(_)) => println(s"Contact already cached. ${contact.id}"); None
      case Success(None) =>
        cache.contactCache.put(contact.id)(contact) match {
          case Success(_) =>
            println(s"Caching new contact. ${contact.id}")
            Some(contact.id)
          case Failure(ex) => println(s"Exception storing contact. ${contact.id} - ${ex}"); None
        }
      case Failure(ex) => println(s"Exception retreiving contact. ${contact.id} - ${ex}"); None
    }
  }

  val writeToDBFromCache: ContactId => Unit = { id =>
    cache.contactCache.get(id) match {
      case Success(contact) =>
        println(s"Successful cache retrieve. ${id}")
        contact.foreach(writeToDB)
      case Failure(ex) => println(s"Failed cache retrieve. ${id} - ${ex.getMessage}")
    }
  }

  /** Helper function to let the [[writeActor]] know when to stop. */
  private val completionMatcher: PartialFunction[Any, CompletionStrategy] = {
    case Done => CompletionStrategy.immediately
  }

  /** An actor that will receive [[ContactId]]s, look them up in the cache and write to the DB. */
  private val writeActor: ActorRef = {
    Source.actorRef[ContactId](
      completionMatcher,
      failureMatcher = PartialFunction.empty,
      bufferSize = 100,
      overflowStrategy = OverflowStrategy.dropHead
    )
    .map(writeToDBFromCache)
    .to(Sink.ignore)
    .run()
  }

  /**
   * Flow:
   *  - Read [[Coordinate]]s
   *  - Group into subsources for each coordinate (Ormi and Mega)
   *  - Make requests in parallel on each Stream
   *  - Parse as [[Contact]]
   *  - Merge into single Sink that writes to the DB
   */
  val graph: RunnableGraph[NotUsed] = source.take(2)
    .mapConcat(coordinatesToRequestInfo)
    .groupBy(
      maxSubstreams = 2,
      _.source.id,
      allowClosedSubstreamRecreation = true)
    .async
    .flatMapConcat(infoToContactSource)
    .via(Flow.fromFunction[Contact, Option[ContactId]](cacheContact))
    .map(_.foreach(writeActor ! _))
    .to(Sink.ignore)

}
