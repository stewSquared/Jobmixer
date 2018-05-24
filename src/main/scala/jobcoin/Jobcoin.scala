package jobcoin

import java.time.Instant
import java.time.temporal.ChronoUnit

object `package` {
  type Jobcoin = BigDecimal
  type Address = String

  def newAddress(): Address = java.util.UUID.randomUUID().toString()
}

object Jobcoin {
  def apply(n: Int) = BigDecimal(n)
}

case class Transaction(
  timestamp: Instant,
  from: Option[Address],
  to: Address,
  amount: Jobcoin
)

object Transaction {
  private def nowMillis() = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  def now(from: Option[Address], to: Address, amount: Jobcoin) =
    Transaction(nowMillis(), from, to, amount)

}
