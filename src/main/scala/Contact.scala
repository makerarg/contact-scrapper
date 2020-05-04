import eu.timepit.refined.api._
import eu.timepit.refined.string._
import thirdparties.{MegaFlexContact, OrmiFlexContact}

case class Contact[R](
  id: String,
  storeName: Option[String],
  name: String,
  location: Location,
  phoneNumber: Contact.PhoneNumber,
  emailAddress: Contact.EmailAddress,
  website: Option[Contact.Website],
  source: ContactSource[R]
)
object Contact {
  type PhoneNumber = String Refined ValidLong
//  type CellPhoneNumber = PhoneNumber Refined StartsWith["""15"""]
  /** https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address */
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url

  def apply(ormiFlexContact: OrmiFlexContact): Contact[OrmiFlexContact] = ???
  def apply(megaFlexContact: MegaFlexContact): Contact[MegaFlexContact] = ???
}
