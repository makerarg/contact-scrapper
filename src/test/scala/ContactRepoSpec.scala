import cats.effect.{ContextShift, IO}
import com.softwaremill.quicklens._
import org.makerarg.contactscrapper.db.{ContactRepo, TestDBConfig}
import eu.timepit.refined.api.RefType
import org.makerarg.contactscrapper._
import org.makerarg.contactscrapper.model._
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.ExecutionContext

class ContactRepoSpec extends FreeSpec with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val repo = new ContactRepo(new TestDBConfig)

  val phoneNumber: Option[PhoneNumber] = RefType.applyRef[PhoneNumber]("1153539333").toOption
  val website: Option[Website] = RefType.applyRef[Website]("www.website.com").toOption
  val emailOpt: String => Option[EmailAddress] = RefType.applyRef[EmailAddress](_).toOption

  val fullContact: Contact = Contact(
    id = "id",
    storeName = Some("storeName"),
    name = "name",
    location = Some(
      Location(
        address = "address",
        city = Some("city"),
        state = Some("state"),
        country = Some("country"),
        zipCode = Some("zipCode"),
        coordinates = Coordinates(
          latitude = Latitude(-38.431),
          longitude = Longitude(-50.213)
        ),
      )
    ),
    phoneNumber = Seq(phoneNumber).flatten,
    emailAddress = Seq(emailOpt("j@gmail.com"), emailOpt("k@gmail.com")).flatten,
    website = website,
    source = "Test"
  )
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

  "should insert a bunch of contacts" in {
    repo.safeWipe.unsafeRunSync

    for (i <- 1 to 1000) {
      repo.safeInsertContact(
        fullContact
          .modify(_.id).setTo(s"full-${i}")
          .modify(_.emailAddress).setTo(fullContact.emailAddress.flatMap(x => emailOpt(s"$x-$i")))
          .modify(_.phoneNumber).setTo(Seq.empty)
      ).unsafeRunSync
      repo.safeInsertContact(
        minimalContact
          .modify(_.id).setTo(s"min-${i}")
      ).unsafeRunSync
    }

    repo.safeWipe.unsafeRunSync
  }


  "should do it async as well" in {
    repo.safeWipe.unsafeRunSync
    repo.safeInsertContact(fullContact).unsafeRunAsync({
      case Right(_) => println("Write success")
      case Left(_) => println("Write failure")
    })

    Thread.sleep(2000)
    repo.safeWipe.unsafeRunSync()
  }
}
