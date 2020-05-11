import java.io.OutputStream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import io.circe.Decoder
import model.Contact
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.typelevel.jawn.AsyncParser
import thirdparties.RawContact

import scala.language.postfixOps

class StreamingScrapper(implicit actorSystem: ActorSystem) {

  /** Make a Source that will parse [[ByteString]]s and materialize as an [[OutputStream]]  */
  def parsingStream[R <: RawContact](source: Source[ByteString, _])(implicit decoder: Decoder[R]): Source[Contact, _] = {
    source
      .via(Flow.fromFunction[ByteString, ByteString]( bs => {
        println(s"Getting chunks ${bs}")
        bs
      }))
      .via(CirceStreamSupport.decode[R](AsyncParser.UnwrapArray))
      .map(Contact(_))
  }

  /** Make a streamed GET request to the given url and return the source as a Readable object  */
  def requestStreamed(url: String): geny.Readable =
    requests
      .get
      .stream(url, onHeadersReceived = { sh => println(s"SH arrived: $sh")})
}
