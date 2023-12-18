

object ClientHello {
    sealed trait Command
    case object start extends Command
    case class SystemMessage(value: String) extends Command
    case class Message(value: String, from: ActorRef[ServerHello.Command]) extends Command
     final case object FindTheServer extends Command
    private case class ListingResponse(listing: Receptionist.Listing) extends Command

    def apply(): Behavior[ClientHello.Command] =
        Behaviors.setup { context =>
        val upnpRef = context.spawn(Upnp(), Upnp.name)
        upnpRef ! AddPortMapping(20000)

        var counter = 0
        // (1) a ServiceKey is a unique identifier for this actor
        var remoteOpt:Option[ActorRef[ServerHello.Command]] = None 

       // (2) create an ActorRef that can be thought of as a Receptionist
        // Listing “adapter.” this will be used in the next line of code.
        // the ClientHello.ListingResponse(listing) part of the code tells the
        // Receptionist how to get back in touch with us after we contact
        // it in Step 4 below.
        // also, this line of code is long, so i wrapped it onto two lines
        val listingAdapter: ActorRef[Receptionist.Listing] =
            context.messageAdapter { listing =>
                println(s"listingAdapter:listing: ${listing.toString}")
                ClientHello.ListingResponse(listing)
            }
        //(3) send a message to the Receptionist saying that we want
        // to subscribe to events related to ServerHello.ServerKey, which
        // represents the ClientHello actor.
        context.system.receptionist ! Receptionist.Subscribe(ServerHello.ServerKey, listingAdapter)

        Behaviors.receiveMessage { message =>
            message match {
                case ClientHello.start =>
                    context.self ! FindTheServer 
                    for (remote <- remoteOpt){
                        remote ! ServerHello.Message("i am fine", context.self)
                    }
                    Behaviors.same
                // (4) send a Find message to the Receptionist, saying
                    // that we want to find any/all listings related to
                    // Mouth.MouthKey, i.e., the Mouth actor.
                case FindTheServer =>
                    println(s"Clinet Hello: got a FindTheServer message")
                    context.system.receptionist !
                        Receptionist.Find(ServerHello.ServerKey, listingAdapter)

                    Behaviors.same
                    // (5) after Step 4, the Receptionist sends us this
                    // ListingResponse message. the `listings` variable is
                    // a Set of ActorRef of type ServerHello.Command, which
                    // you can interpret as “a set of ServerHello ActorRefs.” for
                    // this example i know that there will be at most one
                    // ServerHello actor, but in other cases there may be more
                    // than one actor in this set.
                case ListingResponse(ServerHello.ServerKey.Listing(listings)) =>
                    val xs: Set[ActorRef[ServerHello.Command]] = listings
                    for (x <- xs) {
                        remoteOpt = Some(x)
                    }
                    Behaviors.same
                case ClientHello.Message(value, from) =>
                    println(s"receive message: ${value}")
                    if (counter < 5) {
                        from ! ServerHello.Message("Hello back to you", context.self)
                        counter += 1
                    }
                    Behaviors.same
                case ClientHello.SystemMessage(value) =>
                    for (remote <- remoteOpt){
                        remote ! ServerHello.Message(value, context.self)
                    }
                    Behaviors.same
            }
        }
    }
}

object Client extends App {
  val greeterMain: ActorSystem[ClientHello.Command] = ActorSystem(ClientHello(), "HelloSystem")
  var text = scala.io.StdIn.readLine("command=")
  while (text != "end"){
    greeterMain ! ClientHello.SystemMessage(text)
    text = scala.io.StdIn.readLine("command=")
  }
 
}
