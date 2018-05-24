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
class SyncMixer(val houseAddress: Address, val client: Client) extends Mixer {
  import java.util.UUID

  private def split(amount: Jobcoin, n: Int): List[Jobcoin] = {
    require(n > 0, s"Can't split more amount into $n")
    // TODO: take random commission

    // Note: Simple division is naive. Without splitting randomly, it's easy to
    // determine source by summing similar transactions.
    val part = (amount / n).setScale(amount.scale + 3, BigDecimal.RoundingMode.DOWN)

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

  def checkForDeposit(depositAddress: Address): Future[Option[Jobcoin]] = {
    client.addressInfo(depositAddress).map(_.transactions.headOption.map(_.amount))
  }

  // Have a new random customer deposit 50 coins
  override def mix(withdrawalAddresses: Set[Address]): Address = {
    val customerAddress = newAddress()
    val depositAddress = newAddress()

    // Todo: deposit happens asynchronously from user input
    def setup() = for {
      _ <- client.create(customerAddress)
      newAddressInfo <- client.addressInfo(customerAddress)
      depositTxn <- client.send(from = customerAddress, to = depositAddress, BigDecimal(50))
    } yield {
      println(s"Created new customer with jbc: $newAddressInfo")
      println(s"Customer deposit detected: $depositTxn")
    }

    // TODO: Poll the deposit address instead
    // Note: `.get` is unsafe without `setup()`
    def getAmount() = checkForDeposit(depositAddress).map(_.get)

    // Note: this block should be triggered in background via polling
    def moveMoney(amount: Jobcoin) = for {
      houseTxn <- client.send(from = depositAddress, to = houseAddress, amount)
      // TODO: Implement a random delay before doling out
      txns <- doleOut(amount, withdrawalAddresses)
    } yield {
      println(s"Moved funds from deposit address to house: $houseTxn")
      println("Dole out transactions:\n- " + txns.mkString("\n- "))
    }

    Await.result(setup(), Duration.Inf)
    Await.result(getAmount.flatMap(moveMoney), Duration.Inf)
    depositAddress
  }
}

object MixerApp extends App {
  val client = new FakeClient()
  val mixer = new SyncMixer("house", client)
  mixer.mix(Set("addr1", "addr2", "addr3"))
}
