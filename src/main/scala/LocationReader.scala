import java.io.File

import akka.NotUsed
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import kantan.csv.{ReadResult, rfc}

import scala.annotation.unused
import scala.util.Try

object LocationReader {
  import kantan.csv.ops._

  /** Iterator that will read csv lines as Strings */
  private val readerIt: Iterator[ReadResult[String]] = new File(getClass.getResource("./provider/barrios.csv").getPath).asCsvReader[String](rfc).iterator
  /** Map those lines to Coordinates */
  private val csvLineToCoordinateParser: ReadResult[String] => Coordinates = { readResult =>
    (for {
      line <- readResult
      splitLine = line.split(" ")
      rawValues = Seq(splitLine(1), splitLine(2).split(",").head)
      values = rawValues.map(_.replaceAll("""\(""", ""))
      coordinates <- Try(values.map(_.toDouble)).toEither
    } yield Coordinates(Latitude(coordinates.head), Longitude(coordinates(1)))) match {
      case Left(err) =>
        println(err)
        Coordinates(Latitude(0), Longitude(0))
      case Right(coordinates) => coordinates
    }
  }

  /** Make a [[Source]] out of the [[CsvReader]] iterator */
  val locationSource: Source[ReadResult[String], NotUsed] = Source.fromIterator(() => readerIt)
  /** Send [[ReadResult]]s through our parser*/
  val parserFlow: Flow[ReadResult[String], Coordinates, NotUsed] = Flow.fromFunction[ReadResult[String], Coordinates](
    LocationReader.csvLineToCoordinateParser
  )

  /** For testing only */
  @unused
  val locationGraph: RunnableGraph[NotUsed] = locationSource
    .via(Flow.fromFunction(csvLineToCoordinateParser))
    .to(Sink.foreach(println(_)))

}
