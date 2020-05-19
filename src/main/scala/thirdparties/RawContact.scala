package thirdparties

trait RawContact {
}

import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._

object RawContact {
  implicit val decodeEvent: Decoder[RawContact] =
    List[Decoder[RawContact]](
      Decoder[OrmiFlexContact].widen,
      Decoder[MegaFlexContact].widen
    ).reduceLeft(_ or _)
}
