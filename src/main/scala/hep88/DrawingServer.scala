package hep88
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}
import com.hep88.Upnp
import com.hep88.Upnp.AddPortMapping
import com.hep88.MyConfiguration
import scalafx.collections.ObservableHashSet

class DrawingServer {
  sealed trait Command
  // protocols
  case class JoinBoard(name: String, from: ActorRef[DrawingClient.Command]) extends Command
  case class Leave(name: String, from: ActorRef[DrawingClient.Command]) extends Command

  val members = new ObservableHashSet[User]()
  members.onChange { (ns, _) =>
    for (member <- ns) {
      member.ref ! DrawingClient.MemberList(members.toList)
    }
  }

  val ServerKey: ServiceKey[DrawingServer.Command] = ServiceKey("DrawingServer")

  def apply(): Behavior[DrawingServer.Command] =
    Behaviors.setup { context =>
      val upnpRef = context.spawn(Upnp(), Upnp.name)
      upnpRef ! AddPortMapping(20000)

      context.system.receptionist ! Receptionist.Register(ServerKey, context.self)

      Behaviors.receiveMessage { message =>
        message match {
          case JoinBoard(name, from) =>
            members += User(name, from)
            from ! DrawingClient.Joined(members.toList)
            Behaviors.same
          case Leave(name, from) =>
            members -= User(name, from)
            Behaviors.same
        }

      }
    }
}

// server driver code
object DrawingServerApp extends App {
  val greeterMain: ActorSystem[DrawingServer.Command] = ActorSystem(DrawingServer(), "HelloSystem")
}