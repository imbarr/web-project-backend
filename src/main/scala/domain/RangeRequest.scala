package domain

import domain.SortingOptions.SortingOptions

case class RangeRequest(sortField: String, order: SortingOptions, start: Int, end: Int) {
  require(end >= start)
}