package domain

object VATOptions extends Enumeration {
  type VATOptions = Value
  val None, `18%`, `10%` = Value
}
