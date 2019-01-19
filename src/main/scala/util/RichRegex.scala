package util

import scala.util.matching.Regex

object RichRegex {
  implicit class RichRegex(val underlying: Regex) extends AnyVal {
      def matches(s: String) = underlying.pattern.matcher(s).matches
  }
}
