package jobcoin

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class MixerSpec extends FlatSpec with Matchers with ScalaFutures {
  override def spanScaleFactor = 500.0

  val client: Client = new RestClient()
  val mixer: Mixer = new AsyncMixer(client, newAddress("test-house"))

  "mixer" should "deposit all JBC into given addresses" in {
    val withdrawalAddresses = (1 to 7).map(n => newAddress(s"out$n"))
    val (depositAddress, pollJob) = mixer.mix(withdrawalAddresses: _*)
    val customerAddress = newAddress("cust")

    Await.result(client.create(customerAddress), Duration.Inf)

    (for {
      depositTxn <- client.send(from = customerAddress,
                                to = depositAddress,
                                Jobcoin(50))
      _ <- pollJob
      accounts <- Future.sequence(withdrawalAddresses.map(client.addressInfo))
    } yield {
      accounts.map(_.balance).sum shouldBe Jobcoin(50)
    }).futureValue

  }
}
