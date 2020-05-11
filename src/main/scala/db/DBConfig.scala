package db


import java.util.concurrent.Executors

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.implicits._
import doobie._
import model.Contact

import scala.concurrent.ExecutionContext

class DBConfig {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://localhost:3306/roofie")

  val transactor: IO[HikariTransactor[IO]] = {
    IO.pure(HikariTransactor.apply[IO](
      new HikariDataSource(config),
      ec,
      Blocker.liftExecutionContext(ec)
    ))
  }
}

object ContactQueries {

  def insertContact(contact: Contact): doobie.Update0 = {
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

  def insertContactEmail(contact: Contact): doobie.Update0 = {
    sql"""
         |INSERT INTO `contact_email_address`(
         |    `id`
         |) VALUES (
         |  ???
         |);
        """.stripMargin
      .update
  }

  def insertContactPhone(contact: Contact): doobie.Update0 = {
    sql"""
         |INSERT INTO `contact_phone_number`(
         |    `id`
         |) VALUES (
         |  ???
         |);
        """.stripMargin
      .update
  }

  def insertContactLocation(contact: Contact): doobie.Update0 = {
    sql"""
         |INSERT INTO `contact_location`(
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
}
