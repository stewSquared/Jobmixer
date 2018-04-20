package jobcoin

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class AddressInfo(
  balance: Jobcoin,
  transactions: List[Transaction] // TODO: Always sorted?
)

trait Client {
  def addressInfo(address: Address): Future[AddressInfo]
  def transactions: Future[List[Transaction]] // TODO: Always sorted?

  // TODO: Can the transaction timestamp be retrieved via callback? If not, use `Unit`
  def send(from: Address, to: Address, amount: Jobcoin): Future[Transaction]
}

class FakeClient extends Client {
  import collection.mutable.Map

  // Note: Unsynchronized mutations here
  private var ledger: List[Transaction] = Nil

  // Note: Unsynchronized mutation here
  private val balances = new Map.WithDefault(Map.empty[Address, Jobcoin], (_ : Address) => BigDecimal(0))

  override def addressInfo(address: Address) = Future.successful(
    AddressInfo(
      balances(address),
      ledger.filter(txn => txn.to == address || txn.from == Some(address))))

  override def transactions = Future.successful(ledger)

  override def send(from: Address, to: Address, amount: Jobcoin) = {
    require(amount > 0, "Amount must be over 0")
    require(balances(from) >= 0, s"$from has no jobcoins!  Is it a new or unused address?")
    require(balances(from) >= amount, s"$from only has ${balances(from)} jobcoins!")

    Future[Transaction] {
      balances(from) -= amount
      balances(to) += amount
      val newTransaction = Transaction(Instant.now(), Some(from), to, amount)
      ledger = newTransaction :: ledger

      newTransaction
    }
  }

  // TODO: move to trait and return Future
  def create(address: Address): Unit = {
    val amount: Jobcoin = BigDecimal(50)
    balances(address) += 50
    // TODO add a constructor with timestamp truncated to millis
    val initialDeposit = Transaction(Instant.now(), from = None, to = address, amount)
    ledger = initialDeposit :: ledger
  }
}
