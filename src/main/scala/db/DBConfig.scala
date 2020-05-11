package db

import cats.effect._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari._
import doobie.implicits._
import doobie.util.transactor.Transactor
import model.Contact.{EmailAddress, PhoneNumber}
import model.{Contact, Location}

import scala.concurrent.ExecutionContext

class DBConfig(implicit val ec: ExecutionContext, cs: ContextShift[IO]) {

  private val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/roofie")
  config.setUsername("root")
  config.setPassword("")

  def transactor: IO[Transactor[IO]] = {
    IO.pure(HikariTransactor.apply[IO](
      new HikariDataSource(config),
      ec,
      Blocker.liftExecutionContext(ec)
    ))
  }
}

object ContactQueries {

  def insertContact(contact: Contact): doobie.Update0 = {
    println(s"Inserting $contact")
    sql"""
         |INSERT INTO `third_party_contact`(
         |    `id`,
         |    `store_name`,
         |    `name`,
         |    `website`,
         |    `source`
         |) VALUES (
         |  ${contact.id},
         |  ${contact.storeName},
         |  ${contact.name},
         |  ${contact.website.map(_.value)},
         |  ${contact.source}
         |);
        """.stripMargin
      .update
  }

  def insertContactEmail(contactId: String, emailAddress: EmailAddress): doobie.Update0 = {
    println(s"Inserting $emailAddress for $contactId")
    sql"""
         |INSERT INTO `contact_email_address`(
         |    `contact_id`,
         |    `email_address`
         |) VALUES (
         |  ${contactId},
         |  ${emailAddress.value}
         |);
        """.stripMargin
      .update
  }

  def insertContactPhone(contactId: String, phoneNumber: PhoneNumber): doobie.Update0 = {
    println(s"Inserting $phoneNumber for $contactId")
    sql"""
         |INSERT INTO `contact_phone_number`(
         |    `contact_id`,
         |    `phone_number`
         |) VALUES (
         |  ${contactId},
         |  ${phoneNumber.value}
         |);
        """.stripMargin
      .update
  }

  def insertContactLocation(contactId: String, location: Location): doobie.Update0 = {
    println(s"Inserting $location for $contactId")
    sql"""
         |INSERT INTO `contact_location`(
         |    `contact_id`,
         |    `address`,
         |    `city`,
         |    `state`,
         |    `country`,
         |    `zip_code`,
         |    `latitude`,
         |    `longitude`
         |) VALUES (
         |  ${contactId},
         |  ${location.address},
         |  ${location.city},
         |  ${location.state},
         |  ${location.country},
         |  ${location.zipCode},
         |  ${location.coordinates.latitude.value},
         |  ${location.coordinates.longitude.value}
         |);
        """.stripMargin
      .update
  }

  def wipe: doobie.Update0 = {
    println(s"Wiping tables")
    sql"""
         |DELETE FROM `third_party_contact`;
        """.stripMargin
      .update
  }
}
