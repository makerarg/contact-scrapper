import java.io.OutputStream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, StreamConverters}
import akka.util.ByteString
import eu.timepit.refined.string.Url
import io.circe.generic.auto._
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import thirdparties.{OrmiFlexContact}

import scala.concurrent.duration._
import scala.language.postfixOps

class StreamingScrapper(implicit actorSystem: ActorSystem) {

  /** Make a Source that will parse [[ByteString]]s and materialize as an [[OutputStream]]  */
  val parsingStream: Source[Unit, OutputStream] = StreamConverters.asOutputStream(20 seconds)
    .via(Flow.fromFunction[ByteString, ByteString]( bs => {
      println(s"Getting chunks ${bs}")
      bs
    }))
    .via(CirceStreamSupport.decode[OrmiFlexContact](AsyncParser.UnwrapArray))
    .via(Flow.fromFunction[OrmiFlexContact, Unit](contact => {
      println(contact)
    }))
//    .to(Sink.ignore)

  /** Make a streamed GET request to the given url and return the source as a Readable object  */
  def requestStreamed(url: String): geny.Readable = requests.get
    .stream(url, onHeadersReceived = { sh => println(s"SH arrived: $sh")})
//    .writeBytesTo(parsingStream.run)
}
