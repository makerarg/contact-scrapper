package org.makerarg.contactscrapper.model

import eu.timepit.refined.api._
import eu.timepit.refined.string._
import org.makerarg.contactscrapper.thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

import scala.util.{Success, Try}

case class Contact(
                    id: String,
                    storeName: Option[String],
                    name: String,
                    location: Option[Location],
                    phoneNumber: Seq[org.makerarg.contactscrapper.model.Contact.PhoneNumber],
                    emailAddress: Seq[Contact.EmailAddress],
                    website: Option[Contact.Website],
                    source: String
)
object Contact {
  /** https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address */
  type EmailAddress = String Refined MatchesRegex["""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""]
  type Website = String Refined Url
  type PhoneNumber = String Refined ValidLong

  val emailOpt: String => Option[EmailAddress] = RefType.applyRef[EmailAddress](_).toOption
  val websiteOpt: String => Option[Website] = RefType.applyRef[Website](_).toOption

  /** Parsers based on the data I was able to see... */
  /** Ormiflex specific */
  val parseStringToEmail: String => Seq[EmailAddress] = { dirtyString =>
    emailOpt(dirtyString) match {
      case Some(email) => Seq(email)
      case None =>
        val cleanSpacesAndMultiWord = dirtyString.trim.split(" ").head
        emailOpt(cleanSpacesAndMultiWord).toSeq
    }
  }

  val parseStoreNameToEmail: String => Seq[EmailAddress] = { dirtyString =>
    emailOpt(dirtyString) match {
      case Some(email) => Seq(email)
      case None =>
        val possibleEmails = dirtyString.trim.split(";").tail.toSeq
        possibleEmails match {
          case x :: Nil => emailOpt(x).toSeq
          case x :: xs => (emailOpt(x) :: xs.map(emailOpt(_))).flatten
          case _ => Seq.empty
        }
    }
  }

  /** General */
  val parseCoordinates: (String, String) => Coordinates = (lat, long) => {
    (Try(lat.toDouble), Try(long.toDouble)) match {
      case (Success(lat), Success(long)) => Coordinates(Latitude(lat), Longitude(long))
      case _ => Coordinates(Latitude(0), Longitude(0))
    }
  }

  def apply[R <: RawContact](rawContact: R): Contact = {
    rawContact match {
      case OrmiFlexContact(address, store, _, address2, city, state, zip, _, lat, long, _, _, email, _, url) =>
        val emailFromName: Seq[EmailAddress] = parseStoreNameToEmail(store)
        val emailAddress: Seq[EmailAddress] = parseStringToEmail(email)
        val website: Option[Website] = websiteOpt(url)

        Contact(
          store,
          Option(store),
          store,
          Some(Location(s"$address / $address2", Option(city), Option(state), None, Option(zip), parseCoordinates(lat, long))),
          Seq.empty,
          emailAddress ++ emailFromName,
          website,
          OrmiFlex.id
        )

      case MegaFlexContact(id, nombre, direccion, contacto, _, mail, web, _, lat, long, products, categories, _, _) =>
        val email = emailOpt(mail)
        val website = websiteOpt(web)

        Contact(
          id,
          Option(nombre),
          contacto,
          Some(Location(direccion, None, None, None, None, parseCoordinates(lat, long))),
          Seq.empty,
          email.toSeq,
          website,
          MegaFlex.id
        )
    }
  }
}
