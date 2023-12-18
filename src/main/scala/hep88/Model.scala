package hep88

case class User(name: String, ref: ActorRef[ChatClient.Command]) {
  override def toString: String = {
    name
  }
}
