import eu.timepit.refined.api._
import eu.timepit.refined.string._

import thirdparties.{MegaFlexContact, OrmiFlexContact}

sealed trait Source
case object OrmiFlex
case object MegaFlex

case class Contact(
  id: String,
  storeName: Option[String],
  name: String,
  location: Location,
  phoneNumber: Contact.PhoneNumber,
  emailAddress: Contact.EmailAddress,
  website: Option[Contact.Website],
  source: Source
)
object Contact {
  type PhoneNumber = String Refined ValidLong
//  type CellPhoneNumber = PhoneNumber Refined StartsWith["""15"""]
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url

  def apply(ormiFlexContact: OrmiFlexContact): Contact = ???
  def apply(megaFlexContact: MegaFlexContact): Contact = ???
}

case class Location(
  address: String,
  city: Option[String],
  state: Option[String],
  country: Option[String],
  zipCode: Option[String],
  latitude: Latitude,
  longitude: Longitude
)
object Location {
  def apply(ormiFlexContact: OrmiFlexContact): Location = ???
  def apply(megaFlexContact: MegaFlexContact): Location = ???
}

case class Latitude(value: BigDecimal) {
  require(value >= -90.0d && value <= 90.0d, "Latitude must be in range (-90.0, 90.0)")
}
object Latitude {
  def apply(value: BigDecimal): Latitude = {
    new Latitude(value.setScale(8, BigDecimal.RoundingMode.HALF_UP))
  }
}
case class Longitude(value: BigDecimal) {
  require(value >= -180.0d && value <= 180.0d, "Longitude must be in range (-180.0, 180.0)")
}
object Longitude {
  def apply(value: BigDecimal): Longitude = {
    new Longitude(value.setScale(8, BigDecimal.RoundingMode.HALF_UP))
  }
}
