package jobcoin

import java.time.Instant

object `package` {
  type Jobcoin = BigDecimal
  type Address = String
}

case class Transaction(
  timestamp: Instant,
  from: Option[Address],
  to: Address,
  amount: Jobcoin
)
