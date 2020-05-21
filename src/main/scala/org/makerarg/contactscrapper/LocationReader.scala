package org.makerarg.contactscrapper

import java.io.File

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import kantan.csv._
import org.makerarg.contactscrapper.model.{Coordinates, Latitude, Longitude}

import scala.util.Try

object LocationReader {
  import kantan.csv.ops._
  /* TODO: Make these defs... can't execute multiple test suites with vals */

  /** Iterators that will read csv lines as their respective case class */
  val CABAReaderIterator: Iterator[ReadResult[CABAData]] = {
    new File("src/main/scala/org/makerarg/contactscrapper/locationprovider/barrios-caba.csv")
      .asCsvReader[CABAData](rfc)
      .iterator
  }
  val PBAReaderIterator: Iterator[ReadResult[PBAData]] = {
    new File("src/main/scala/org/makerarg/contactscrapper/locationprovider/mapa-judicial-pba.csv").asCsvReader[PBAData](
      rfc
        .withHeader(true)
        .withCellSeparator(';')
    ).iterator
  }

  /** Map [[ReadResult]] lines to [[Coordinates]] */
  private val CABACsvLineToCoordinateParser: ReadResult[CABAData] => Coordinates = { readResult =>
    println("CABA row")
    (for {
      line <- readResult
      splitLine = line.polygon.split(" ")
      rawValues = Seq(splitLine(1), splitLine(2).split(",").head)
      values = rawValues.map(_.replaceAll("""\(""", ""))
      coordinates <- Try(values.map(_.toDouble)).toEither
    } yield Coordinates(Latitude(coordinates(1)), Longitude(coordinates.head)))
      .getOrElse(defaultCoordinates)
  }
  private val PBACsvLineToCoordinateParser: ReadResult[PBAData] => Coordinates = { readResult =>
    println("PBA row")
    (for {
      line <- readResult
    } yield Coordinates(Latitude(line.lat), Longitude(line.long)))
      .getOrElse(defaultCoordinates)
  }

  /** Make a [[Source]] out of the [[CsvReader]] iterators */
  val CABALocationSource: Source[ReadResult[CABAData], NotUsed] = Source.fromIterator(() => CABAReaderIterator)
  val PBALocationSource: Source[ReadResult[PBAData], NotUsed] = Source.fromIterator(() => PBAReaderIterator)

  /** Send [[ReadResult]]s through our parsers */
  val CABAParserFlow: Flow[ReadResult[CABAData], Coordinates, NotUsed] = {
    Flow.fromFunction[ReadResult[CABAData], Coordinates](
      LocationReader.CABACsvLineToCoordinateParser
    )
  }
  val PBAParserFlow: Flow[ReadResult[PBAData], Coordinates, NotUsed] = {
    Flow.fromFunction[ReadResult[PBAData], Coordinates](
      LocationReader.PBACsvLineToCoordinateParser
    )
  }

  /** Make each [[Source]] go through its parser */
  val CABASource: Source[Coordinates, NotUsed] = CABALocationSource.via(CABAParserFlow)
  val PBASource: Source[Coordinates, NotUsed] = PBALocationSource.via(PBAParserFlow)

  /** Retrieve a [[Source]] that will stream every read pair of [[Coordinates]] */
  val coordinateSource: Source[Coordinates, NotUsed] = CABASource.merge(PBASource)

}

case class CABAData(polygon: String, neighbourhood: String, i: Int, d1: Double, d2: Double)
object CABAData {
  implicit val CABARowDecoder: RowDecoder[CABAData] = RowDecoder.ordered {
    (p: String, n: String, i: Int, d1: Double, d2: Double) =>
      CABAData(p, n, i, d1, d2)
  }
}
case class PBAData(id: Int, t: String, d1: String, d2: String, address: String, n1: String, n2: String, cp: String, lat: Double, long: Double)
object PBAData {
  implicit val PBARowDecoder: RowDecoder[PBAData] = RowDecoder.ordered {
    (id: Int, t: String, d1: String, d2: String, a: String, n1: String, n2: String, cp: String, lat: Double, long: Double) =>
      PBAData(id, t, d1, d2, a, n1, n2, cp, lat, long)
  }
}
