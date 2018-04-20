package jobcoin

import java.time.Instant

object `package` {
  type Jobcoin = BigDecimal
  type Address = String

  def newAddress(): Address = java.util.UUID.randomUUID().toString()
}

case class Transaction(
  timestamp: Instant,
  from: Option[Address],
  to: Address,
  amount: Jobcoin
)
