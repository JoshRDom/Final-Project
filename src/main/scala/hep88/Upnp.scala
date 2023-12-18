package hep88

import java.net.InetAddress

object Cling {    
    // UPnP discovery is asynchronous, we need a callback
    val listener: RegistryListener = new RegistryListener() {

        def remoteDeviceDiscoveryStarted(registry: Registry,  device: RemoteDevice) {
            println("Discovery started: " + device.getDisplayString())
        }

        def remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception) {
            println("Discovery failed: " + device.getDisplayString() + " => " + ex)
         }

        def remoteDeviceAdded( registry: Registry,  device: RemoteDevice) {
            println(
            "Remote device available: " + device.getDisplayString()
            )
        }

        def remoteDeviceUpdated( registry: Registry,  device: RemoteDevice) {
            println(
            "Remote device updated: " + device.getDisplayString()
            );
        }

        def remoteDeviceRemoved( registry: Registry,  device: RemoteDevice) {
            println(
            "Remote device removed: " + device.getDisplayString()
            );
        }

        def localDeviceAdded( registry: Registry,  device: LocalDevice) {
            println(
            "Local device added: " + device.getDisplayString()
            );
        }

        def localDeviceRemoved( registry: Registry,  device: LocalDevice) {
            println(
            "Local device removed: " + device.getDisplayString()
            );
        }

        def beforeShutdown( registry: Registry) {
            println(
            "Before shutdown, the registry has devices: "
            + registry.getDevices().size()
            );
        }

        def afterShutdown() {
            println("Shutdown of registry complete!");

        }
    }
}

object Upnp {
    val name = "UpnpManager"
    trait Command 
    case class AddPortMapping(port: Int) extends Command

    def apply(): Behavior[Upnp.Command] = {
        Behaviors.setup { context => 
            context.log.info("Starting Cling...")
            val upnpService: UpnpService = new UpnpServiceImpl(Cling.listener)
            Behaviors.receiveMessage[Upnp.Command] {
                case AddPortMapping(num) =>
                    val mapping = List(
                        new PortMapping(num, InetAddress.getLocalHost().getHostAddress(),
                                PortMapping.Protocol.TCP, " TCP POT Forwarding"),
                        new PortMapping(num, InetAddress.getLocalHost().getHostAddress(),
                            PortMapping.Protocol.UDP, " UDP POT Forwarding")
                    )               
                    val registryListener: RegistryListener = new PortMappingListener(mapping.toArray);
                    upnpService.getRegistry().addListener(registryListener);
                    upnpService.getControlPoint().search(new STAllHeader());
                    Behaviors.same
                case _ =>
                    Behaviors.unhandled
            }
            .receiveSignal {
                case (context, PostStop) =>
                    upnpService.shutdown()
                    // Release all resources and advertise BYEBYE to other UPnP devices
                    println("Stopping Cling...")
                    Behaviors.same
            }
        }
    }
}
