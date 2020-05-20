package org.makerarg.contactscrapper.scrapper

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{RunnableGraph, Sink}
import io.circe.Decoder
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.ContactRepo
import org.makerarg.contactscrapper.model.Contact
import org.makerarg.contactscrapper.thirdparties.RawContact

class SyncScrapper(
 val cache: CaffeineCache,
 val repo: ContactRepo
)(implicit val actorSystem: ActorSystem) extends Scrapper {

  /** Make a GET request to the given url and return the source as a Readable object  */
  def request[R <: RawContact](url: String)(implicit decoder: Decoder[R]): Seq[Contact] = {
    import io.circe.parser.decode

    println(s"Making request to ${url}")
    val res = requests.get(url).text
    decode[Seq[R]](res) match {
      case Left(err) => println(err); Seq.empty
      case Right(contacts) => contacts.map(Contact(_))
    }
  }

  def infoToContacts[R <: RawContact](info: RequestInfo)(implicit decoder: Decoder[R]): Seq[Contact] = {
    request[R](info.source.url(info.coordinates))
  }

  /**
   * Simplest graph.
   */
  val graph: RunnableGraph[NotUsed] = source
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
    .to(Sink.foreach(writeToDB))

}
