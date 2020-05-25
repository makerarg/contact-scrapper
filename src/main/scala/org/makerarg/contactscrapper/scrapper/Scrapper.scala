package org.makerarg.contactscrapper.scrapper

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{RunnableGraph, Source}
import org.makerarg.contactscrapper.LocationReader
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.ContactRepo
import org.makerarg.contactscrapper.model.{Contact, ContactSource, Coordinates, MegaFlex, OrmiFlex}

trait Scrapper {

  implicit val actorSystem: ActorSystem

  val cache: CaffeineCache
  val repo: ContactRepo
  val graph: RunnableGraph[NotUsed]

  val source: Source[Coordinates, NotUsed] = LocationReader.coordinateSource
  val writeToDB: Contact => Unit = { contact =>
    repo.safeInsertContact(contact).attempt.unsafeRunSync() match {
      case Left(ex) => println(s"Write error. ${ex}")
      case Right(_) => println(s"Write successful. ${contact.id}")
    }
  }
  val writeToDBAsync: Contact => Unit = { contact =>
    repo.safeInsertContact(contact).unsafeRunAsync({
      case Right(_) => println(s"Write success. ContactId: ${contact.id}")
      case Left(_) => println("Write failure")
    })
  }

  val coordinatesToRequestInfo: Coordinates => List[RequestInfo] = { coordinates =>
    List(
      RequestInfo(OrmiFlex, coordinates),
      RequestInfo(MegaFlex, coordinates)
    )
  }
}

case class RequestInfo(source: ContactSource, coordinates: Coordinates)
