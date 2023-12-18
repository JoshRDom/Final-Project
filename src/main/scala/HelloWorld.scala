import Echo.Message

//describe an actor //create an actor
object Echo {
  var name: String = "hello"

  //coomunication protocol
  final case class Message(value: String)

  def apply(): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage { message: Message =>
        println("receive " + message.value) //void
        Behaviors.same  //behavior next next meesaae
      }
    }
}

object MyApp extends App {
  val greeterMain: ActorSystem[Echo.Message] = ActorSystem(Echo(), "AkkaQuickStart")

  greeterMain ! Message("hello word")
  greeterMain ! Message("Charles 2")
  greeterMain ! Message("Charles 2")

}
