package com.hep88
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}
import com.hep88.Upnp
import com.hep88.Upnp.AddPortMapping
import com.hep88.MyConfiguration
import scalafx.collections.ObservableHashSet

object DrawingBoardServer {
  sealed trait Command

  // Server protocols
  case class ServerConnect(name: String, from: ActorRef[DrawingBoardClient.Command]) extends Command
  case class ServerDisconnect(name: String, from: ActorRef[DrawingBoardClient.Command]) extends Command

  // masterlist of members
  val members = new ObservableHashSet[User]()
  // method to update everyone in chat on new masterlist
  members.onChange{(hs, x) =>
    for(member <- hs){
      member.ref ! com.hep88.DrawingBoardClient.MemberList(members.toList)
    }
  }

  // Server key
  val ServerKey: ServiceKey[DrawingBoardServer.Command] = ServiceKey("DrawingBoardServer")

  def apply(): Behavior[DrawingBoardServer.Command] =
    Behaviors.setup { context =>
      val upnpRef = context.spawn(Upnp(), Upnp.name)
      upnpRef ! AddPortMapping(20000)

      context.system.receptionist ! Receptionist.Register(ServerKey, context.self)

      Behaviors.receiveMessage { message =>
        message match {
          // Server protocol algorithm
          case ServerConnect(name, from) =>
            members += User(name, from)
            from ! com.hep88.DrawingBoardClient.Joined(members.toList)
            for (user <- members.toList) {
              user.ref ! com.hep88.DrawingBoardClient.Message(name + " has joined the chat", from)
            }
            Behaviors.same
          case ServerDisconnect(name, from) =>
            members -= User(name, from)
            from ! com.hep88.DrawingBoardClient.Joined(members.toList)
            for (user <- members.toList) {
              user.ref ! com.hep88.DrawingBoardClient.Message(name + " has left the chat", from)
            }
            Behaviors.same
        }
      }
    }
}

object DrawingBoardServerApp extends App {
  val greeterMain: ActorSystem[DrawingBoardServer.Command] = ActorSystem(DrawingBoardServer(), "DrawingBoardSystem")

}
