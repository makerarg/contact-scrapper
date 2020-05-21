package org.makerarg

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Url, ValidLong}

package object contactscrapper {

  /** https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address */
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url
  type PhoneNumber = String Refined ValidLong

  type ContactId = String
}
