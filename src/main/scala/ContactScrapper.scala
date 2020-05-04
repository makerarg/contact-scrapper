import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, StreamConverters}
import akka.util.ByteString
import io.circe.generic.auto._
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser

import scala.concurrent.duration._
import scala.language.postfixOps

object ContactScrapper extends App {

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

//  val ormiRes = requests.get(OrmiFlex.HOST)
//  val jsString = ormiRes.text
//  val ormiContacts = decode[Seq[OrmiFlexContact]](jsString)
//  println(ormiContacts)
}
