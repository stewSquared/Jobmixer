## Jobmixer - A Jobcoin Mixer

To use, install `sbt` and run:

```bash
sbt "run foo bar baz"
```

where foo, bar, and baz are your withdrawal addresses.

This will start a new mixer and generate a deposit address.  Once the
deposit is detected, the mixer will transfer funds to your withdrawal
addresses, then exit.

Alternatively, just run `sbt console`, then paste something like the following.

```scala
import jobcoin._
val client = new RestClient()
val mixer = new AsyncMixer(client)
val (depositAddress, _) = mixer.mix("foo", "bar", "baz")
val customerAddress = newAddress("customer")

client.create(customerAddress)
client.send(from = customerAddress, to = depositAddress, Jobcoin(13))
```

Use `sbt scalafmt` and `sbt test` to format and run tests, respectively.
