package jobcoin

import java.util.concurrent.Future

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
