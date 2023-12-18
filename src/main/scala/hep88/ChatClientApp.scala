package hep88


object Client extends JFXApp {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val greeterMain: ActorSystem[ChatClient.Command] = ActorSystem(ChatClient(), "HelloSystem")

  greeterMain ! ChatClient.start


  val loader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(getClass.getResourceAsStream("view/MainWindow.fxml"))
  val border: scalafx.scene.layout.BorderPane = loader.getRoot[javafx.scene.layout.BorderPane]()
  val control = loader.getController[com.hep88.view.MainWindowController#Controller]()
  control.chatClientRef = Option(greeterMain)
  val cssResource = getClass.getResource("view/DarkTheme.css")
  stage = new PrimaryStage() {
    scene = new Scene(){
      root = border
      stylesheets = List(cssResource.toExternalForm)
    }
  }

  stage.onCloseRequest = handle( {
    greeterMain.terminate
  })


}