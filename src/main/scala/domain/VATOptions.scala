package domain

object VATOptions extends Enumeration {
  type VATOptions = Value
  val None = Value
  val `18%` = Value("18%")
  val `10%` = Value("10%")
}
