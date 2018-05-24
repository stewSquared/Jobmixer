package jobcoin

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future._
import com.softwaremill.sttp.circe._
import io.circe._
import io.circe.generic.auto._

class RestClient extends Client with Codecs {

  implicit val backend = AsyncHttpClientFutureBackend()

  val apiEndpoint = "http://jobcoin.gemini.com/fragility/api"

  override def addressInfo(address: Address): Future[AddressInfo] = {
    sttp
      .get(uri"$apiEndpoint/addresses/$address")
      .response(asJson[AddressInfo])
      .send()
      .flatMap(_.unsafeBody.fold(Future.failed, Future.successful))
  }

  override def transactions: Future[List[Transaction]] = {
    sttp
      .get(uri"$apiEndpoint/transactions")
      .response(asJson[List[Transaction]])
      .send()
      .flatMap(_.unsafeBody.fold(Future.failed, Future.successful))
  }

  override def send(from: Address,
                    to: Address,
                    amount: Jobcoin): Future[Transaction] = {
    val sendJBC = sttp
      .post(uri"$apiEndpoint/transactions")
      .body("fromAddress" -> from,
            "toAddress" -> to,
            "amount" -> amount.toString)
      .send()
      .map(_.unsafeBody)

    sendJBC.flatMap(_ => latestTransaction(to).map(_.get))
  }

  override def create(address: Address): Future[Transaction] = {
    val createJBC = sttp
      .post(uri"https://jobcoin.gemini.com/fragility/create")
      .body("address" -> address)
      .send()
      .filter(!_.isServerError)
    // Note: create side-effect still happens on a 404,
    // so we ignore everything but server error

    createJBC.flatMap(_ => latestTransaction(address).map(_.get))
  }
}

trait Codecs {
  import cats.syntax.either._

  implicit val encodeInsatnt: Encoder[Instant] =
    Encoder.encodeString.contramap(_.toString)
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap {
    str =>
      Either.catchNonFatal(Instant.parse(str)).leftMap(t => "Instant")
  }

  implicit val decodeTransaction: Decoder[Transaction] =
    Decoder.forProduct4("timestamp", "fromAddress", "toAddress", "amount")(
      Transaction.apply)
}
