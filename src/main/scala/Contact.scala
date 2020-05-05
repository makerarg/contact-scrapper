import eu.timepit.refined.api._
import eu.timepit.refined.string._
import thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

case class Contact[R <: RawContact](
  id: String,
  storeName: Option[String],
  name: String,
//  location: Location,
//  phoneNumber: Contact.PhoneNumber,
  emailAddress: Option[Contact.EmailAddress],
  website: Option[Contact.Website],
  source: String
)
object Contact {
  type PhoneNumber = String Refined ValidLong
  /** https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address */
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url

  def apply[R <: RawContact](rawContact: R): Contact[R] = {
    rawContact match {
      case OrmiFlexContact(_, store, _, _, _, _, _, _, _, _, _, _, email, _, url) =>
        val emailAddress = RefType.applyRef[EmailAddress](email).toOption
        val website = RefType.applyRef[Website](url).toOption

        Contact(
          java.util.UUID.randomUUID().toString,
          Option(store),
          store,
          emailAddress,
          website,
          OrmiFlex.id
        )

      case MegaFlexContact(id, nombre, _, contacto, _, mail, web, _, _, _, _, _, _, _) =>
        val email = RefType.applyRef[EmailAddress](mail).toOption
        val website = RefType.applyRef[Website](web).toOption

        Contact(
          id,
          Option(nombre),
          contacto,
          email,
          website,
          MegaFlex.id
        )
    }
  }
}
