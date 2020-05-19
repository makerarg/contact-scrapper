import cats.effect._
import cats.implicits._
import db.DBConfig
import doobie.Update
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query
import model.Contact

class ContactRepo(dBConfig: DBConfig) {

  import ContactQueries._

  def safeInsertContact(contact: Contact): IO[Unit] = {
    println(s"safeInsertContact inserting ${contact}")
    (for {
      _ <- insertIO(contact)
      _ <- insertEmailsIO(contact)
      _ <- insertPhonesIO(contact)
      _ <- insertLocationIO(contact)
    } yield ()).transact(dBConfig.transactor)
  }

  def safeWipe: IO[Unit] = {
    val t = dBConfig.transactor
    for {
      _ <- wipe.run.transact(t)
    } yield ()
  }
}

object ContactQueries {
  type ContactRow = (String, Option[String], String, Option[String], String)
  type EmailRow = (String, String)
  type PhoneRow = (String, String)
  type LocationRow = (String, String, Option[String], Option[String], Option[String], Option[String], BigDecimal, BigDecimal)

  implicit class ContactOps(private val contact: Contact) extends AnyVal {
    def toContactRow: ContactRow = contact match {
      case Contact(id, storeName, name, _, _, _, website, source) =>
        (id, storeName, name, website.map(_.value), source)
    }

    def toEmailRows: List[EmailRow] = contact match {
      case Contact(id, _, _, _, _, emailAddresses, _, _) =>
        emailAddresses.map(e => (id, e.value)).toList
    }

    def toPhoneRows: List[PhoneRow] = contact match {
      case Contact(id, _, _, _, phoneNumbers, _, _, _) =>
        phoneNumbers.map(p => (id, p.value)).toList
    }

    def toLocationRow: Option[LocationRow] = contact match {
      case Contact(id, _, _, locationOpt, _, _, _, _) =>
        locationOpt.map { l =>
          (id, l.address, l.city, l.state, l.country, l.zipCode, l.coordinates.latitude.value, l.coordinates.longitude.value)
        }
    }
  }

  def insertIO(contact: Contact): ConnectionIO[Unit] = {
    println(s"insertIO ${contact.id}")
    val sql =
      """
        |INSERT INTO `third_party_contact`(
        |    `id`,
        |    `store_name`,
        |    `name`,
        |    `website`,
        |    `source`
        |) VALUES (?, ?, ?, ?, ?);
      """.stripMargin

    Query[ContactRow, Unit](sql).unique(contact.toContactRow)
  }

  def insertMany(contacts: List[Contact]): ConnectionIO[Int] = {
    val sql =
      """
        |INSERT INTO `third_party_contact`(
        |    `id`,
        |    `store_name`,
        |    `name`,
        |    `website`,
        |    `source`
        |) VALUES (?, ?, ?, ?, ?)
        |""".stripMargin

    Update[ContactRow](sql).updateMany(contacts.map(_.toContactRow))
  }

  def insertEmailsIO(contact: Contact): ConnectionIO[Int] = {
    println(s"insertEmailsIO ${contact.id}")
    val sql =
      """
         |INSERT INTO `contact_email_address`(
         |    `contact_id`,
         |    `email_address`
         |) VALUES (?, ?)
        """.stripMargin

    Update[EmailRow](sql).updateMany(contact.toEmailRows)
  }

  def insertPhonesIO(contact: Contact): ConnectionIO[Int] = {
    println(s"insertPhonesIO ${contact.id}")
    val sql =
      """
        |INSERT INTO `contact_phone_number`(
        |    `contact_id`,
        |    `phone_number`
        |) VALUES (?, ?)
        """.stripMargin

    Update[PhoneRow](sql).updateMany(contact.toPhoneRows)
  }

  def insertLocationIO(contact: Contact): ConnectionIO[Unit] = {
    println(s"insertLocationIO ${contact.id}")

    contact.toLocationRow match {
      case Some(locationRow) =>
        val sql =
          """
            |INSERT INTO `contact_location`(
            |    `contact_id`,
            |    `address`,
            |    `city`,
            |    `state`,
            |    `country`,
            |    `zip_code`,
            |    `latitude`,
            |    `longitude`
            |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          """.stripMargin
        Query[LocationRow, Unit](sql).unique(locationRow)

      case None =>
        AsyncConnectionIO.pure(())
    }
  }

  def wipe: doobie.Update0 = {
    println(s"Wiping tables")
    sql"""
         |DELETE FROM `third_party_contact`;
        """.stripMargin
      .update
  }
}

