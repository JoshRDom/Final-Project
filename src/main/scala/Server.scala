
object ServerHello {
  sealed trait Command
  case class Message(value: String, from: ActorRef[ClientHello.Command]) extends Command
  val ServerKey: ServiceKey[ServerHello.Command] = ServiceKey("Server")

  def apply(): Behavior[ServerHello.Command] =
    Behaviors.setup { context =>
      val upnpRef = context.spawn(Upnp(), Upnp.name)
      upnpRef ! AddPortMapping(20000)

      context.system.receptionist ! Receptionist.Register(ServerKey, context.self)
      
      Behaviors.receiveMessage { message =>
        message match {
            case Message(value, from) =>
                println(s"Server received message '${value}'")
                from ! ClientHello.Message("how are you", context.self)
            Behaviors.same
        }
      }
    }
}

object Server extends App {
  val greeterMain: ActorSystem[ServerHello.Command] = ActorSystem(ServerHello(), "HelloSystem")

}
