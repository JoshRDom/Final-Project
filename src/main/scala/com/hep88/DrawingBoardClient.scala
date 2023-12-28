package com.hep88
import akka.actor.typed.{ActorRef, PostStop, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}
import akka.cluster.typed._
import akka.{ actor => classic }
import akka.actor.typed.scaladsl.adapter._
import scalafx.collections.ObservableHashSet
import scalafx.application.Platform
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.Address
import com.hep88.Upnp._

object DrawingBoardClient {
  sealed trait Command
  // internal protocols
  case object start extends Command
  case class StartJoin(name: String) extends Command
  final case class SendMessageL(target: ActorRef[DrawingBoardClient.Command], content: String) extends Command

  final case object FindTheServer extends Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  private final case class MemberChange(event: MemberEvent) extends Command
  private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Command
  val members = new ObservableHashSet[User]()

  val unreachables = new ObservableHashSet[Address]()
  unreachables.onChange{(ns, _) =>
    Platform.runLater {
      DrawingBoardApp.control.updateList(members.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
    }
  }

  members.onChange{(ns, _) =>
    Platform.runLater {
      DrawingBoardApp.control.updateList(ns.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
    }
  }

  //chat protocol
  final case class MemberList(list: Iterable[User]) extends Command
  final case class Joined(list: Iterable[User]) extends Command
  final case class Message(msg: String, from: ActorRef[DrawingBoardClient.Command]) extends Command

  var defaultBehavior: Option[Behavior[DrawingBoardClient.Command]] = None
  var remoteOpt: Option[ActorRef[DrawingBoardServer.Command]] = None
  var nameOpt: Option[String] = None

  def messageStarted(): Behavior[DrawingBoardClient.Command] = Behaviors.receive[DrawingBoardClient.Command] { (context, message) =>
    message match {
      case SendMessageL(target, content) =>
        target ! Message(nameOpt.get + ": " + content, context.self)
        Behaviors.same
      case Message(msg, from) =>
        Platform.runLater {
          DrawingBoardApp.control.addText(msg)
        }
        Behaviors.same
      case MemberList(list: Iterable[User]) =>
        members.clear()
        members ++= list
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      for (name <- nameOpt;
           remote <- remoteOpt){
        remote ! DrawingBoardServer.ServerDisconnect(name, context.self)
      }
      defaultBehavior.getOrElse(Behaviors.same)
  }

  def apply(): Behavior[DrawingBoardClient.Command] =
    Behaviors.setup { context =>
      var counter = 0
      // (1) a ServiceKey is a unique identifier for this actor

      val upnpRef = context.spawn(Upnp(), Upnp.name)
      upnpRef ! AddPortMapping(0)


      val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
      Cluster(context.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

      // (2) create an ActorRef that can be thought of as a Receptionist
      // Listing “adapter.” this will be used in the next line of code.
      // the DrawingBoardClient.ListingResponse(listing) part of the code tells the
      // Receptionist how to get back in touch with us after we contact
      // it in Step 4 below.
      // also, this line of code is long, so i wrapped it onto two lines
      val listingAdapter: ActorRef[Receptionist.Listing] =
      context.messageAdapter { listing =>
        println(s"listingAdapter:listing: ${listing.toString}")
        DrawingBoardClient.ListingResponse(listing)
      }

      //(3) send a message to the Receptionist saying that we want
      // to subscribe to events related to ServerHello.ServerKey, which
      // represents the DrawingBoardClient actor.
      context.system.receptionist ! Receptionist.Subscribe(DrawingBoardServer.ServerKey, listingAdapter)
      //context.actorOf(RemoteRouterConfig(RoundRobinPool(5), addresses).props(Props[DrawingBoardClient.TestActorClassic]()), "testA")
      defaultBehavior = Some(Behaviors.receiveMessage[DrawingBoardClient.Command] { message =>
        message match {
          case DrawingBoardClient.start =>

            context.self ! FindTheServer
            Behaviors.same
          // (4) send a Find message to the Receptionist, saying
          // that we want to find any/all listings related to
          // Mouth.MouthKey, i.e., the Mouth actor.
          case FindTheServer =>
            println(s"Client Hello: got a FindTheServer message")
            context.system.receptionist !
              Receptionist.Find(DrawingBoardServer.ServerKey, listingAdapter)

            Behaviors.same
          // (5) after Step 4, the Receptionist sends us this
          // ListingResponse message. the `listings` variable is
          // a Set of ActorRef of type ServerHello.Command, which
          // you can interpret as “a set of ServerHello ActorRefs.” for
          // this example i know that there will be at most one
          // ServerHello actor, but in other cases there may be more
          // than one actor in this set.
          case ListingResponse(DrawingBoardServer.ServerKey.Listing(listings)) =>
            val xs: Set[ActorRef[DrawingBoardServer.Command]] = listings
            for (x <- xs) {
              remoteOpt = Some(x)
            }
            Behaviors.same
          case StartJoin(name) =>
            nameOpt = Option(name)
            remoteOpt.map ( _ ! DrawingBoardServer.ServerConnect(name, context.self))
            Behaviors.same
          case Message(msg, from) =>
            Platform.runLater {
              DrawingBoardApp.control.addText(msg)
            }
            Behaviors.same
          case DrawingBoardClient.Joined(x) =>
            DrawingBoardApp.control.enableChat()
            members.clear()
            members ++= x
            messageStarted()
          case ReachabilityChange(reachabilityEvent) =>
            reachabilityEvent match {
              case UnreachableMember(member) =>
                unreachables += member.address
                Behaviors.same
              case ReachableMember(member) =>
                unreachables -= member.address
                Behaviors.same
            }
          case _=>
            Behaviors.unhandled

        }
      }.receiveSignal {
        case (context, PostStop) =>
          for (name <- nameOpt;
               remote <- remoteOpt){
            remote ! DrawingBoardServer.ServerDisconnect(name, context.self)
          }
          Behaviors.same
      })
      defaultBehavior.get
    }
}
