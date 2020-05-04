import Contact.{EmailAddress, PhoneNumber, Website}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, StreamConverters}
import akka.util.ByteString
import eu.timepit.refined.api._
import io.circe.generic.auto._
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import thirdparties.{OrmiFlex, OrmiFlexContact}

import scala.concurrent.duration._
import scala.language.postfixOps

object ContactScrapper extends App {

//  StreamingScrapper.streamedReq

  val p: Either[String, PhoneNumber] = RefType.applyRef[PhoneNumber]("213123")
  val e: Either[String, EmailAddress] = RefType.applyRef[EmailAddress]("jeronimo.carlos@ing.austral.edu.ar")
  val email: Either[String, EmailAddress] = RefType.applyRef[EmailAddress]("jeronimo.carlos@hotmail.com")
  val w: Either[String, Website] = RefType.applyRef[Website]("http://www.apliancor.com.ar/")

  p match {
    case Left(x) => println(x)
    case _ => ()
  }

  Seq(e, email) map {
    case Left(invalidEmail) => println(s"${invalidEmail}")
    case Right(goodEmail) => println(s"Great email! ${goodEmail}")
  }
}

object StreamingScrapper {

  implicit val actorSystem = ActorSystem("ScrapSys")

  val stream = StreamConverters.asOutputStream(20 seconds)
    .via(Flow.fromFunction[ByteString, ByteString]( bs => {
      println(s"Getting chunks ${bs}")
      bs
    }))
    .via(CirceStreamSupport.decode[OrmiFlexContact](AsyncParser.UnwrapArray))
    .via(Flow.fromFunction[OrmiFlexContact, Unit](contact => {
      println(contact)
    }))
    .to(Sink.ignore)

  val streamedReq = requests.get
    .stream(OrmiFlex.HOST, onHeadersReceived = { sh => println(s"SH arrived: ${sh}")})
    .writeBytesTo(stream.run)
}
