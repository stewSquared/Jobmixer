package jobcoin

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }
import scala.concurrent.ExecutionContext.Implicits.global

class ClientSpec extends FlatSpec with Matchers with ScalaFutures {
  val client = new FakeClient()

  "New address" should "have 0 balance and no transactions" in {
    client.addressInfo(newAddress()).futureValue shouldBe AddressInfo(0, Nil)
  }

  it should "be able to start with new coins" in {
    val addr = newAddress()
    client.create(addr)

    client.addressInfo(addr).futureValue shouldBe AddressInfo(50, Nil)
  }

  it should "be able to recive new coins" in {
    val addr1 = newAddress()
    val addr2 = newAddress()
    client.create(addr1)

    (for {
      txn <- client.send(from = addr1, to = addr2, BigDecimal(20))
      info1 <- client.addressInfo(addr1)
      info2 <- client.addressInfo(addr2)
    } yield {
      info1 shouldBe AddressInfo(BigDecimal(30), transactions = List(txn))
      info2 shouldBe AddressInfo(BigDecimal(20), transactions = List(txn))
    }).futureValue
  }
}
