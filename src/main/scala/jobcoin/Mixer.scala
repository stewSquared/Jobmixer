package jobcoin

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

trait Mixer {
  val houseAddress: Address

  /** Immediately return a fresh deposit address,
    * then begin a job to poll for deposits to said address.
    */
  def mix(withdrawalAddresses: Address*): (Address, Future[Unit])
}

class AsyncMixer(client: Client,
                 val houseAddress: Address = newAddress("house"))
    extends Mixer {
  private def split(amount: Jobcoin, n: Int): List[Jobcoin] = {
    require(n > 0, s"Can't split more amount into $n")
    // TODO: take random commission

    // Note: Simple division is naive. Without splitting randomly, it's easy to
    // determine source by summing similar transactions.
    val part =
      (amount / n).setScale(amount.scale + 3, BigDecimal.RoundingMode.DOWN)

    (amount - part * (n - 1)) :: List.fill(n - 1)(part)
  }

  def doleOut(amount: Jobcoin,
              withdrawalAddresses: Seq[Address]): Future[Seq[Transaction]] = {
    val withdrawalAmounts = split(amount, withdrawalAddresses.size)

    Future.sequence {
      (withdrawalAmounts zip withdrawalAddresses).map {
        case (payAmount, payAddress) =>
          client.send(from = houseAddress, to = payAddress, payAmount)
      }
    }
  }

  def pollForDeposit(depositAddress: Address): Future[Transaction] = {
    client.latestTransaction(depositAddress).flatMap { deposit =>
      deposit.map(Future.successful).getOrElse {
        blocking(Thread.sleep(1000))
        pollForDeposit(depositAddress)
      }
    }
  }

  override def mix(withdrawalAddresses: Address*): (Address, Future[Unit]) = {
    require(withdrawalAddresses.distinct.size == withdrawalAddresses.size)
    val depositAddress = newAddress("deposit")

    val pollJob = for {
      depositTxn <- pollForDeposit(depositAddress)
      houseTxn <- client
        .send(from = depositAddress, to = houseAddress, depositTxn.amount)
      txns <- doleOut(depositTxn.amount, withdrawalAddresses)
    } yield {
      println(s"Customer deposit detected: $depositTxn")
      println(s"Moved deposit to house: $houseTxn")
      println("Dole out transactions:\n - " + txns.mkString("\n - "))
    }

    (depositAddress, pollJob)
  }
}

object MixerApp extends App {
  val withdrawalAddresses = args

  val houseAddress = newAddress("house")
  val mixer = new AsyncMixer(new RestClient(), houseAddress)
  println(s"Started mixer with address: $houseAddress")

  val (depositAddress, pollJob) = mixer.mix(withdrawalAddresses: _*)
  println(s"Your deposit address is: $depositAddress")
  println(
    "Please use the UI at https://jobcoin.gemini.com/fragility to deposit your Jobcoin.")
  println(
    "They will eventually be transferred into these addresses:\n - " + withdrawalAddresses
      .mkString("\n - "))
  pollJob.onComplete(_.fold(throw _, _ => System.exit(_)))
}
