package jobcoin

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait Mixer {
  /** Immediately return a fresh deposit address, 
    * then begin a job to poll for deposits to said address.
    */
  def mix(withdrawalAddresses: Set[Address]): Address

  val houseAddress: Address
}

/* non-interactive Mixer implementation that needs no polling of a customer
 */
class SyncMixer(val houseAddress: Address) extends Mixer {
  import java.util.UUID

  val client = new FakeClient

  private def split(amount: Jobcoin, n: Int): List[Jobcoin] = {
    require(n > 0, s"Can't split more amount into $n")
    // TODO: take random commission

    // Note: Simple division is naive. Without splitting randomly, it's easy to
    // determine source by summing similar transactions.
    val part = (amount / n).setScale(amount.scale + 3)

    (amount - part * (n-1)) :: List.fill(n - 1)(part)
  }

  // TODO: Make this async
  def doleOut(amount: Jobcoin, withdrawalAddresses: Set[Address]): Future[List[Transaction]] = {
    val withdrawalAmounts = split(amount, withdrawalAddresses.size)

    Future.sequence {
      (withdrawalAmounts zip withdrawalAddresses).map { case (payAmount, payAddress) =>
        client.send(from = houseAddress, to = payAddress, payAmount)
      }
    }
  }

  // Have a new random customer deposit 50 coins
  override def mix(withdrawalAddresses: Set[Address]): Address = {
    val customerAddress = newAddress()
    val depositAddress = newAddress()

    // Todo: Parameterize Client; customer should be initialized separately.
    client.create(customerAddress)
    locally {
      val newAddressInfo = Await.result(client.addressInfo(customerAddress), Duration.Inf)
      println(s"Created new customer with jbc: $newAddressInfo")
    } 

    // TODO: Poll the deposit address instead
    val amount = BigDecimal(50)

    for {
      depositTxn <- client.send(from = customerAddress, to = depositAddress, amount)
      houseTxn <- client.send(from = depositAddress, to = houseAddress, amount)
    } {
      println(s"Customer deposit detected: $depositTxn")
      println(s"Moved funds from deposit address to house: $houseTxn")
    }

    // TODO: Implement a random delay before doling out
    // Note: this will become an background task via polling
    Await.result(doleOut(amount, withdrawalAddresses).map((txns: List[Transaction]) =>
      println("Dole out transactions:\n- " + txns.mkString("\n- "))
    ), Duration.Inf)

    depositAddress
  }
}
