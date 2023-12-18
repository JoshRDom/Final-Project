package hep88.view
@sfxml
class MainWindowController(private val txtName: TextField,
private val lblStatus: Label, private val listUser: ListView[User],
private val listMessage: ListView[String],
private val txtMessage: TextField) {

    var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

    val receivedText: ObservableBuffer[String] =  new ObservableBuffer[String]()

    listMessage.items = receivedText

    def handleJoin(action: ActionEvent): Unit = {
        if(txtName != null)
          chatClientRef map (_ ! ChatClient.StartJoin(txtName.text()))
    }

    def displayStatus(text: String): Unit = {
        lblStatus.text = text
    }
  def updateList(x: Iterable[User]): Unit ={
    listUser.items = new ObservableBuffer[User]() ++= x
  }
  def handleSend(actionEvent: ActionEvent): Unit ={

    if (listUser.selectionModel().selectedIndex.value >= 0){
      Client.greeterMain ! ChatClient.SendMessageL(listUser.selectionModel().selectedItem.value.ref,
        txtMessage.text())
    }
  }
  def addText(text: String): Unit = {
      receivedText += text
  }

}