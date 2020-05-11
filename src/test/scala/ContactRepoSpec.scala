import db.DBConfig
import eu.timepit.refined.api.RefType
import model.Contact.{EmailAddress, PhoneNumber, Website, websiteOpt}
import model._

object ContactRepoSpec extends App {

  val dbConfig = new DBConfig
  val repo = new ContactRepo(dbConfig)

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

  repo.safeInsertContact(fullContact).unsafeRunSync
}
