package jobcoin

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class MixerSpec extends FlatSpec with Matchers with ScalaFutures {
  override def spanScaleFactor = 100.0

  val client: Client = new FakeClient()
  val mixer: Mixer = new AsyncMixer(client, newAddress("test-house"))

  "mixer" should "deposit all JBC into given addresses" in {
    val (depositAddress, pollJob) = mixer.mix(Set("addr1", "addr2", "addr3"))
    val customerAddress = newAddress()

    Await.result(client.create(customerAddress), Duration.Inf)

    (for {
      depositTxn <- client.send(from = customerAddress, to = depositAddress, Jobcoin(50))
      _ <- pollJob
      AddressInfo(bal1, _) <- client.addressInfo("addr1")
      AddressInfo(bal2, _) <- client.addressInfo("addr2")
      AddressInfo(bal3, _) <- client.addressInfo("addr3")
    } yield {
      (bal1 + bal2 + bal3) shouldBe Jobcoin(50)
    }).futureValue

  }
}
