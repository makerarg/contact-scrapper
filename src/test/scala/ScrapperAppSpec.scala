import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.effect.{ContextShift, IO}
import org.makerarg.contactscrapper.LocationReader
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.{ContactRepo, TestDBConfig}
import org.makerarg.contactscrapper.model._
import org.makerarg.contactscrapper.scrapper.{RequestInfo, StreamingScrapper}
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.ExecutionContext

class ScrapperAppSpec extends FreeSpec with Matchers {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new TestDBConfig)


  val streamingScrapper = new StreamingScrapper(cache, repo) {
    val singleItemSource: Source[Coordinates, NotUsed] = LocationReader.coordinateSource.take(1)

    override val infoToContactSource: RequestInfo => Source[Contact, _] = { info: RequestInfo =>
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
        .mapConcat(streamingScrapper.coordinatesToRequestInfo)
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
        .mapConcat(streamingScrapper.coordinatesToRequestInfo)
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
        .mapConcat(streamingScrapper.coordinatesToRequestInfo)
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
        .mapConcat(streamingScrapper.coordinatesToRequestInfo)
        .groupBy(
          maxSubstreams = 2,
          _.source,
          allowClosedSubstreamRecreation = true)
    }
  }
}
