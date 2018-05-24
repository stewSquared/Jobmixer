package jobcoin

import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait Mixer {
  /** Immediately return a fresh deposit address, 
    * then begin a job to poll for deposits to said address.
    */
  def mix(withdrawalAddresses: Set[Address]): (Address, Future[Unit])

  val houseAddress: Address
}

class AsyncMixer(val client: Client, val houseAddress: Address = newAddress("house")) extends Mixer {
  import java.util.UUID

  private def split(amount: Jobcoin, n: Int): List[Jobcoin] = {
    require(n > 0, s"Can't split more amount into $n")
    // TODO: take random commission

    // Note: Simple division is naive. Without splitting randomly, it's easy to
    // determine source by summing similar transactions.
    val part = (amount / n).setScale(amount.scale + 3, BigDecimal.RoundingMode.DOWN)

    (amount - part * (n-1)) :: List.fill(n - 1)(part)
  }

  def doleOut(amount: Jobcoin, withdrawalAddresses: Set[Address]): Future[List[Transaction]] = {
    val withdrawalAmounts = split(amount, withdrawalAddresses.size)

    Future.sequence {
      (withdrawalAmounts zip withdrawalAddresses).map { case (payAmount, payAddress) =>
        client.send(from = houseAddress, to = payAddress, payAmount)
      }
    }
  }

  def checkForDeposit(depositAddress: Address): Future[Option[Transaction]] = {
    client.addressInfo(depositAddress).map(_.transactions.headOption)
  }

  def pollForDeposit(depositAddress: Address): Future[Transaction] = {
    println(s"Checking $depositAddress for deposit...")
    checkForDeposit(depositAddress).flatMap { deposit =>
      deposit.map(Future.successful).getOrElse {
        blocking(Thread.sleep(1000))
        pollForDeposit(depositAddress)
      }
    }
  }

  override def mix(withdrawalAddresses: Set[Address]): (Address, Future[Unit]) = {
    val depositAddress = newAddress()

    val pollJob: Future[Unit] = pollForDeposit(depositAddress) flatMap { depositTxn =>
      for {
        houseTxn <- client.send(from = depositAddress, to = houseAddress, depositTxn.amount)
        // TODO: Implement a random delay before doling out
        txns <- doleOut(depositTxn.amount, withdrawalAddresses)
      } yield {
        println(s"Customer deposit detected: $depositTxn")
        println(s"Moved funds from deposit address to house: $houseTxn")
        println("Dole out transactions:\n- " + txns.mkString("\n- "))
      }
    }

    (depositAddress, pollJob)
  }
}

object MixerApp extends App {
  val client = new FakeClient()
  val mixer = new AsyncMixer("house", client)
  val (depositAddress, _) = mixer.mix(Set("addr1", "addr2", "addr3"))
  val customerAddress = newAddress()

  Await.result(client.create(customerAddress), Duration.Inf)
  Await.result(client.send(from = customerAddress, to = depositAddress, Jobcoin(50)), Duration.Inf)
}
