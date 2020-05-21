package org.makerarg

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Url, ValidLong}
import org.makerarg.contactscrapper.model.{Coordinates, Latitude, Longitude}

package object contactscrapper {

  val magicNumber = 27
  /** https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address */
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url
  type PhoneNumber = String Refined ValidLong

  type ContactId = String

  val defaultCoordinates: Coordinates = Coordinates(Latitude(0), Longitude(0))
  val lastCoordinates: Coordinates = Coordinates(Latitude(magicNumber), Longitude(magicNumber))
}
