package hep88

object ChatClient {
    sealed trait Command
    //internal protocla
    case object start extends Command
    case class StartJoin(name: String) extends Command
   final case class SendMessageL(target: ActorRef[ChatClient.Command], content: String) extends Command

    final case object FindTheServer extends Command
    private case class ListingResponse(listing: Receptionist.Listing) extends Command
    private final case class MemberChange(event: MemberEvent) extends Command
    private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Command
    val members = new ObservableHashSet[User]()

  val unreachables = new ObservableHashSet[Address]()
    unreachables.onChange{(ns, _) =>
        Platform.runLater {
            Client.control.updateList(members.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
        }
    }

  members.onChange{(ns, _) =>
    Platform.runLater {
        Client.control.updateList(ns.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
    }  
  }

//chat protocol
  final case class MemberList(list: Iterable[User]) extends Command
  final case class Joined(list: Iterable[User]) extends Command
  final case class Message(msg: String, from: ActorRef[ChatClient.Command]) extends Command

  var defaultBehavior: Option[Behavior[ChatClient.Command]] = None
  var remoteOpt: Option[ActorRef[ChatServer.Command]] = None 
  var nameOpt: Option[String] = None

    def messageStarted(): Behavior[ChatClient.Command] = Behaviors.receive[ChatClient.Command] { (context, message) => 
        message match {
            case SendMessageL(target, content) =>
                target ! Message(content, context.self)
                Behaviors.same
            case Message(msg, from) =>
                Platform.runLater {
                    Client.control.addText(msg)
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
            remote ! ChatServer.Leave(name, context.self)
            }
            defaultBehavior.getOrElse(Behaviors.same)
    }

    def apply(): Behavior[ChatClient.Command] =
        Behaviors.setup { context =>
        var counter = 0
        // (1) a ServiceKey is a unique identifier for this actor

       val upnpRef = context.spawn(Upnp(), Upnp.name)
        upnpRef ! AddPortMapping(20000)
        
          
    val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
    Cluster(context.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

       // (2) create an ActorRef that can be thought of as a Receptionist
        // Listing “adapter.” this will be used in the next line of code.
        // the ChatClient.ListingResponse(listing) part of the code tells the
        // Receptionist how to get back in touch with us after we contact
        // it in Step 4 below.
        // also, this line of code is long, so i wrapped it onto two lines
        val listingAdapter: ActorRef[Receptionist.Listing] =
            context.messageAdapter { listing =>
                println(s"listingAdapter:listing: ${listing.toString}")
                ChatClient.ListingResponse(listing)
            }
        //(3) send a message to the Receptionist saying that we want
        // to subscribe to events related to ServerHello.ServerKey, which
        // represents the ChatClient actor.
        context.system.receptionist ! Receptionist.Subscribe(ChatServer.ServerKey, listingAdapter)
        //context.actorOf(RemoteRouterConfig(RoundRobinPool(5), addresses).props(Props[ChatClient.TestActorClassic]()), "testA")
        defaultBehavior = Some(Behaviors.receiveMessage[ChatClient.Command] { message =>
            message match {
                case ChatClient.start =>
                    
                    context.self ! FindTheServer 
                    Behaviors.same
                // (4) send a Find message to the Receptionist, saying
                    // that we want to find any/all listings related to
                    // Mouth.MouthKey, i.e., the Mouth actor.
                case FindTheServer =>
                    println(s"Clinet Hello: got a FindTheServer message")
                    context.system.receptionist !
                        Receptionist.Find(ChatServer.ServerKey, listingAdapter)

                    Behaviors.same
                    // (5) after Step 4, the Receptionist sends us this
                    // ListingResponse message. the `listings` variable is
                    // a Set of ActorRef of type ServerHello.Command, which
                    // you can interpret as “a set of ServerHello ActorRefs.” for
                    // this example i know that there will be at most one
                    // ServerHello actor, but in other cases there may be more
                    // than one actor in this set.
                case ListingResponse(ChatServer.ServerKey.Listing(listings)) =>
                    val xs: Set[ActorRef[ChatServer.Command]] = listings
                    for (x <- xs) {
                        remoteOpt = Some(x)
                    }
                    Behaviors.same
                case StartJoin(name) =>
                    nameOpt = Option(name)
                    remoteOpt.map ( _ ! ChatServer.JoinChat(name, context.self))
                     Behaviors.same
                case ChatClient.Joined(x) =>
                    Platform.runLater {
                        Client.control.displayStatus("joined")
                    }
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
                remote ! ChatServer.Leave(name, context.self)
            }
            Behaviors.same
        })
        defaultBehavior.get
    }
}
