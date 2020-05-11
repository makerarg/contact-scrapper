import cats.effect.{ContextShift, IO}
import eu.timepit.refined.api.RefType
import model.Contact.{EmailAddress, PhoneNumber, Website}
import model._

import scala.concurrent.ExecutionContext

object ContactRepoSpec extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val repo = new ContactRepo

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

  repo.safeWipe.unsafeRunSync
  repo.safeInsertContact(fullContact).unsafeRunSync
  repo.safeInsertContact(minimalContact).unsafeRunSync
  repo.safeWipe.unsafeRunSync
}
