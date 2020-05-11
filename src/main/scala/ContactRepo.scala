import db.DBConfig
import model.Contact
import doobie.implicits._
import db.ContactQueries._
import cats.effect._


class ContactRepo(dBConfig: DBConfig) {

  import cats.implicits._

  def safeInsertContact(contact: Contact) = {
    for {
      t <- dBConfig.transactor
      _ <- insertContact(contact).run.transact(t)
      emailQueries: List[IO[Int]] = contact.emailAddress.map(insertContactEmail(contact.id, _).run.transact(t)).toList
      _ <- emailQueries.sequence
      phoneQueries: List[IO[Int]] = contact.phoneNumber.map(insertContactPhone(contact.id, _).run.transact(t)).toList
      _ <- phoneQueries.sequence
      _ <- contact.location.map(insertContactLocation(contact.id, _).run.transact(t)).getOrElse(IO.unit)
    } yield ()
  }
}
