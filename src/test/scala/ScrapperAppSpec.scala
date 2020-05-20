import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import cats.effect.{ContextShift, IO}
import org.makerarg.contactscrapper.LocationReader
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.{ContactRepo, DBConfig}
import org.makerarg.contactscrapper.model._
import org.makerarg.contactscrapper.scrapper.{RequestInfo, StreamingScrapper}
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.ExecutionContext

object DummyScrapperApp extends App {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new DBConfig)
  val streamingScrapper = new StreamingScrapper(cache, repo)

  val minimalContact: Contact = Contact(
    id = "id2",
    storeName = None,
    name = "name",
    location = None,
    phoneNumber = Seq.empty,
    emailAddress = Seq.empty,
    website = None,
    source = "Test"
  )

  case class FakeRequestInfo(url: String)
  /**
   * Flow:
   *  - Read [[Location]]s
   *  - Make a request to each [[ContactSource]] for each [[Location]]
   *    - Parse [[ByteString]]s as [[Contact]]s
   *  - Merge incoming streams into single [[Sink]]
   *  - Store unique contacts
   */
  var n = 0
  LocationReader.coordinateSource
    .via(streamingScrapper.coordinatesToRequestInfoFlow)
    .mapConcat(identity)
    .via(Flow.fromFunction[RequestInfo, (geny.Readable, String)] { info =>
      n += 1
      println(n)
      println(s"making request to $info.url")
      ("some string readable", info.source.id)
    })
    /*
    .flatMapConcat { _ =>
      println("parsing 'contact'")
      Source.fromIterator(() => List(minimalContact).iterator)
    }
    .via(Flow.fromFunction[Contact, Option[String]](contact => {
      org.makerarg.contactscrapper.cache.contactCache.put(contact.id)(contact) match {
        case Success(_) => Some(contact.id)
        case Failure(ex) =>
          println(s"contactCache.put failed with ex ${ex.getMessage}")
          None
      }
    }))

     */
    .to(Sink.foreach(println))
    /*
    .to(Sink.foreach {
      case Some(id) =>
        org.makerarg.contactscrapper.cache.contactCache.get(id) match {
          case Success(contact) =>
            contact.map(repo.safeInsertContact(_).unsafeRunAsync {
              case Right(_) => ()
              case Left(ex) =>
//                println(s"DB write failed for ${id}")
//                println(ex.getMessage)
                ()
            })
          case Failure(ex) =>
//            println(s"Cache retrieval failed for ${id}")
//            println(ex.getMessage)
            ()
        }
      case _ => ()
    })

     */
    .run()

}

class ScrapperAppSpec extends FreeSpec with Matchers {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new DBConfig)


  val streamingScrapper = new StreamingScrapper(cache, repo) {
    val singleItemSource: Source[Coordinates, NotUsed] = LocationReader.coordinateSource.take(1)

    val infoToContactSource: RequestInfo => Source[Contact, _] = { info: RequestInfo =>
      println(info)
      val url = info.source.url(info.coordinates)
      println(s"making request to $url")

      info.source.id match {
        case OrmiFlex.id => Source.fromIterator(() => List(minimalContact).iterator)
        case MegaFlex.id => Source.fromIterator(() => List(minimalContact).iterator)
        case _ =>
          println("empty source")
          Source.empty
      }
    }
  }

  val minimalContact: Contact = Contact(
    id = "id2",
    storeName = None,
    name = "name",
    location = None,
    phoneNumber = Seq.empty,
    emailAddress = Seq.empty,
    website = None,
    source = "OrmiFlex"
  )

  "ScrapperAppSpec" - {
    "should go through every coordinate combination" in {
      var n = 0
      LocationReader.coordinateSource
        .via(streamingScrapper.coordinatesToRequestInfoFlow)
        .mapConcat(identity)
        .flatMapConcat { _ =>
          println("parsing 'contact'")
          n += 1
          Source.fromIterator(() => List(minimalContact).iterator)
        }
        .to(Sink.ignore)
        .run()

      Thread.sleep(2000)
      n shouldBe 3742
    }

    "should go through every contact source" in {
      var n = 0
      LocationReader.coordinateSource
        .via(streamingScrapper.coordinatesToRequestInfoFlow)
        .mapConcat(identity)
        .flatMapConcat(streamingScrapper.infoToContactSource)
        .via(Flow.fromFunction(_ => n += 1))
        .to(Sink.ignore)
        .run

      Thread.sleep(2000)
      n shouldBe 3742
    }

    "should split in substreams and still go through every source" in {
      var n = 0

      LocationReader.coordinateSource
        .via(streamingScrapper.coordinatesToRequestInfoFlow)
        .mapConcat(identity)
        .groupBy(
          maxSubstreams = 2,
          _.source,
          allowClosedSubstreamRecreation = true)
        .flatMapConcat(streamingScrapper.infoToContactSource)
        .via(Flow.fromFunction(_ => n += 1))
        .mergeSubstreams
        .to(Sink.foreach(println))
        .run

      Thread.sleep(2500)
      n shouldBe 3742
    }


    "should parse results" in {
      streamingScrapper.singleItemSource
        .via(streamingScrapper.coordinatesToRequestInfoFlow)
        .mapConcat(identity)
        .groupBy(
          maxSubstreams = 2,
          _.source,
          allowClosedSubstreamRecreation = true)
    }
  }
}
