import db.{ContactQueries, DBConfig}
import cats.effect._
import cats.implicits._
import doobie.implicits._
import model.Contact

class ContactRepo {

  import ContactQueries._

  val dBConfig = new DBConfig

  def safeInsertContact(contact: Contact): IO[Unit] = {
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

  def safeWipe: IO[Unit] = {
    for {
      t <- dBConfig.transactor
      _ <- wipe.run.transact(t)
    } yield ()
  }
}
