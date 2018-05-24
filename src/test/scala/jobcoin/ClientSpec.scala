package jobcoin

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Inside, FlatSpec, Matchers }
import scala.concurrent.ExecutionContext.Implicits.global

class ClientSpec extends FlatSpec with Matchers with ScalaFutures with Inside {
  val client = new FakeClient()

  "New address" should "have 0 balance and no transactions" in {
    client.addressInfo(newAddress()).futureValue shouldBe AddressInfo(Jobcoin(0), Nil)
  }

  it should "be able to be loaded with new coins" in {
    val addr = newAddress()
    client.create(addr)

    whenReady(client.addressInfo(addr)) {
      x => inside(x) { case AddressInfo(balance, List(initialTransaction)) =>
        balance shouldBe Jobcoin(50)
        inside(initialTransaction) {
          case Transaction(_, from, to, amount) =>
            amount shouldBe balance
            from shouldBe None
            amount shouldBe balance
        }
      }
    }
  }

  it should "be able to receive new coins" in {
    val addr1 = newAddress()
    val addr2 = newAddress()
    client.create(addr1)

    (for {
      sendTxn <- client.send(from = addr1, to = addr2, Jobcoin(20))
      info1 <- client.addressInfo(addr1)
      info2 <- client.addressInfo(addr2)
    } yield {
      inside(info1) {
        case AddressInfo(balance, List(create, latest)) =>
          balance shouldBe Jobcoin(30)
          sendTxn shouldBe latest
      }
      info2 shouldBe AddressInfo(Jobcoin(20), transactions = List(sendTxn))
    }).futureValue
  }
}
