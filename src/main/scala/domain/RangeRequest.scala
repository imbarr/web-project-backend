package domain

import domain.SortingOptions.SortingOptions

import scala.util.Try

object RangeRequest {
  def unapply(v: Map[String, String]) =
    Try(RangeRequest(v("sortField"), SortingOptions.withName(v("order")), v("start").toInt, v("end").toInt)).toOption
}

case class RangeRequest(sortField: String, order: SortingOptions, start: Int, end: Int) {
  require(end >= start)
}