package jobcoin

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object `package` {
  type Jobcoin = BigDecimal
  type Address = String

  def newAddress(): Address = UUID.randomUUID().toString()
  def newAddress(description: String): Address =
    s"$description@${UUID.randomUUID()}"
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
  def now(from: Option[Address], to: Address, amount: Jobcoin) =
    Transaction(Instant.now().truncatedTo(ChronoUnit.MILLIS), from, to, amount)

}
