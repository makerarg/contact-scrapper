import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import cache.CaffeineCache
import cats.effect.{ContextShift, IO}
import db.DBConfig
import model.{Contact, ContactSource, Coordinates, Location, MegaFlex, OrmiFlex}
import thirdparties.{MegaFlexContact, OrmiFlexContact}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ScrapperAppSpec extends App {
  implicit val actorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val streamingScrapper = new StreamingScrapper
  val cache = new CaffeineCache
  val repo = new ContactRepo(new DBConfig)

  import io.circe.generic.auto._
  import scalacache.modes.try_._

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
    .via(Flow.fromFunction[Coordinates, List[FakeRequestInfo]]{ coordinates =>
      println(s"$coordinates")
      List(
        FakeRequestInfo("some url"),
        FakeRequestInfo("some other url")
      )
    })
    .mapConcat(identity)
    .via(Flow.fromFunction[FakeRequestInfo, (geny.Readable, String)] { info =>
      n += 1
      println(n)
      println(s"making request to $info.url")
      ("some string readable", info.url)
    })
    .flatMapConcat { _ =>
      println("parsing 'contact'")
      Source.fromIterator(() => List(minimalContact).iterator)
    }
    .via(Flow.fromFunction[Contact, Option[String]](contact => {
      cache.contactCache.put(contact.id)(contact) match {
        case Success(_) => Some(contact.id)
        case Failure(ex) =>
          println(s"contactCache.put failed with ex ${ex.getMessage}")
          None
      }
    }))
    .to(Sink.foreach {
      case Some(id) =>
        cache.contactCache.get(id) match {
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
    .run()

}
