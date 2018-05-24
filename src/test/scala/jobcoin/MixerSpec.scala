package jobcoin

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global

class MixerSpec extends FlatSpec with Matchers with ScalaFutures {
  val client = new FakeClient()
  val mixer = new SyncMixer("house", client)

  "mixer" should "deposit all JBC into given addresses" in {
    mixer.mix(Set("addr1", "addr2", "addr3"))

    Thread.sleep(1000)

    (for {
      AddressInfo(bal1, _) <- client.addressInfo("addr1")
      AddressInfo(bal2, _) <- client.addressInfo("addr2")
      AddressInfo(bal3, _) <- client.addressInfo("addr3")
    } yield {
      (bal1 + bal2 + bal3) shouldBe BigDecimal(50)
    }).futureValue

  }
}
