import db.{ContactQueries, DBConfig}
import cats.effect._
import cats.implicits._
import doobie.implicits._
import model.Contact

class ContactRepo(dBConfig: DBConfig) {

  import ContactQueries._

  def safeInsertContact(contact: Contact): IO[Unit] = {
    println(s"safeInsertContact inserting ${contact}")
    for {
      _ <- insertContact(contact).run.transact(dBConfig.transactor)
      emailQueries: List[IO[Int]] = contact.emailAddress.map(insertContactEmail(contact.id, _).run.transact(dBConfig.transactor)).toList
      _ <- emailQueries.sequence
      phoneQueries: List[IO[Int]] = contact.phoneNumber.map(insertContactPhone(contact.id, _).run.transact(dBConfig.transactor)).toList
      _ <- phoneQueries.sequence
      _ <- contact.location.map(insertContactLocation(contact.id, _).run.transact(dBConfig.transactor)).getOrElse(IO.unit)
    } yield ()
  }

  def safeWipe: IO[Unit] = {
    val t = dBConfig.transactor
    for {
      _ <- wipe.run.transact(t)
    } yield ()
  }
}
