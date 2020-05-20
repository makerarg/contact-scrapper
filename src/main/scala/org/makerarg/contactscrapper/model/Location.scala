package org.makerarg.contactscrapper.model

import org.makerarg.contactscrapper.thirdparties.{MegaFlexContact, OrmiFlexContact}

case class Location(
  address: String,
  city: Option[String],
  state: Option[String],
  country: Option[String],
  zipCode: Option[String],
  coordinates: Coordinates
)
object Location {
  def apply(ormiFlexContact: OrmiFlexContact): Location = ???
  def apply(megaFlexContact: MegaFlexContact): Location = ???
}

case class Coordinates(latitude: Latitude, longitude: Longitude)

case class Latitude(value: BigDecimal) {
  require(value >= -90.0d && value <= 90.0d, "org.makerarg.contactscrapper.model.Latitude must be in range (-90.0, 90.0)")
}
object Latitude {
  def apply(value: BigDecimal): Latitude = {
    new Latitude(value.setScale(8, BigDecimal.RoundingMode.HALF_UP))
  }
}
case class Longitude(value: BigDecimal) {
  require(value >= -180.0d && value <= 180.0d, "org.makerarg.contactscrapper.model.Longitude must be in range (-180.0, 180.0)")
}
object Longitude {
  def apply(value: BigDecimal): Longitude = {
    new Longitude(value.setScale(8, BigDecimal.RoundingMode.HALF_UP))
  }
}
