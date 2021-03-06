package jobcoin

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class AddressInfo(
    balance: Jobcoin,
    transactions: List[Transaction]
)

trait Client {
  def addressInfo(address: Address): Future[AddressInfo]
  def transactions: Future[List[Transaction]]

  def send(from: Address, to: Address, amount: Jobcoin): Future[Transaction]

  def create(address: Address): Future[Transaction]

  def latestTransaction(address: Address): Future[Option[Transaction]] = {
    addressInfo(address).map(_.transactions.lastOption)
  }
}

// Naive `Client` implementation for testing
class FakeClient extends Client {
  import collection.mutable.Map

  // Note: Unsynchronized mutations here
  private var ledger: List[Transaction] = Nil
  private val balances =
    new Map.WithDefault(Map.empty[Address, Jobcoin], (_: Address) => Jobcoin(0))

  override def addressInfo(address: Address) =
    Future.successful(
      AddressInfo(balances(address),
                  ledger
                    .filter(txn =>
                      txn.to == address || txn.from == Some(address))
                    .reverse))

  override def transactions = Future.successful(ledger.reverse)

  override def send(from: Address, to: Address, amount: Jobcoin) = {
    require(amount > 0, "Amount must be over 0")
    require(balances(from) >= 0,
            s"$from has no jobcoins!  Is it a new or unused address?")
    require(balances(from) >= amount,
            s"$from only has ${balances(from)} jobcoins!")

    Future[Transaction] {
      balances(from) -= amount
      balances(to) += amount
      val newTransaction = Transaction.now(Some(from), to, amount)
      ledger = newTransaction :: ledger

      newTransaction
    }
  }

  override def create(address: Address): Future[Transaction] = {
    val initialDeposit = Transaction.now(from = None, to = address, Jobcoin(50))
    balances(address) += initialDeposit.amount

    ledger = initialDeposit :: ledger
    Future.successful(initialDeposit)
  }
}
