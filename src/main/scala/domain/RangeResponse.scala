package domain

case class RangeResponse[T](count: Int, result: Seq[T])
