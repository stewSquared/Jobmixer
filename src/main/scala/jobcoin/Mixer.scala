package jobcoin

trait Mixer {
  /** Immediately return a fresh deposit address, 
    * then begin a job to poll for deposits to said address.
    */
  def mix(withdrawalAddresses: Set[Address]): Address

  val houseAddress: Address
}
