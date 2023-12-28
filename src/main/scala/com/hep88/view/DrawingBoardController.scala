package com.hep88.view
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseDragEvent, MouseEvent}
import scalafx.scene.control.{ColorPicker, ComboBox, ListView, Slider, TextField}
import com.hep88.DrawingBoardClient
import com.hep88.User
import com.hep88.DrawingBoardApp
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color
import scalafx.scene.shape.Line

@sfxml
class DrawingBoardController( // chat stuff
                              private val listUser: ListView[User],
                              private val listMessage: ListView[String],
                              private val txtMessage: TextField,

                              // canvas
                              private val canvas: Canvas,

                              // tools stuff
                              private val colorPicker: ColorPicker,
                              private val toolComboBox: ComboBox[String],
                              private val widthSlider: Slider,
                              private val backgroundColorPicker: ColorPicker ){

  // CONTROLLER VARIABLES
  // chat stuff
  var drawingBoardClientRef: Option[ActorRef[DrawingBoardClient.Command]] = None
  val receivedText: ObservableBuffer[String] = new ObservableBuffer[String]()

  // canvas stuff
  println(canvas)
  val gc = canvas.getGraphicsContext2D()
  val line = new Line()

  // VARIABLE INITIALISATIONS
  // chat stuff
  listMessage.items = receivedText

  // canvas stuff
  gc.setFill(Color.White) // Set your initial canvas color here
  gc.fillRect(0, 0, canvas.width.value, canvas.height.value)

  // tools stuff
  colorPicker.value = Color.Black
  var currentColor = colorPicker.value
  toolComboBox.items = ObservableBuffer[String]("Pen", "Eraser", "Line")
  toolComboBox.value = toolComboBox.items.getValue.get(0) // initial tool
  var currentTool = toolComboBox.value

  // FUNCTION DEFINITIONS
  // chat stuff
  def updateList(x: Iterable[User]): Unit ={
    listUser.items = new ObservableBuffer[User]() ++= x
  }

  // text field onActions
  def handleJoin(action: ActionEvent): Unit = {
    if (txtMessage != null)
      drawingBoardClientRef map (_ ! DrawingBoardClient.StartJoin(txtMessage.text()))
  }

  def handleSend(actionEvent: ActionEvent): Unit ={
    for( user <- listUser.items.getValue ) {
      DrawingBoardApp.greeterMain ! DrawingBoardClient.SendMessageL(user.ref,
        txtMessage.text())
    }
    txtMessage.text = ""
  }

  def enableChat(): Unit = {
    txtMessage.setOnAction(handleSend)
    txtMessage.setPromptText("Message")
    txtMessage.text = ""
  }

  def addText(text: String): Unit = {
    receivedText += text
  }

  // canvas stuff
  // canvas onMouseClick event
  canvas.setOnMousePressed((e) => {
    currentTool.value match {
      case "Pen" =>
        gc.setStroke(currentColor.getValue)
        gc.beginPath
        gc.lineTo(e.getX, e.getY)

      case "Eraser" =>
        val lineWidth = gc.getLineWidth
        gc.fillRect(e.getX - lineWidth / 2, e.getY - lineWidth / 2, lineWidth, lineWidth)

      case "Line" =>
        gc.setStroke(currentColor.getValue)
        line.setStartX(e.getX)
        line.setStartY(e.getY)
    }
  })

  // canvas onMouseDrag event
  canvas.setOnMouseDragged((e) => {
    currentTool.value match {
      case "Pen" =>
        gc.lineTo(e.getX, e.getY)
        gc.stroke

      case "Eraser" =>
        val lineWidth = gc.getLineWidth
        gc.fillRect(e.getX - lineWidth / 2, e.getY - lineWidth / 2, lineWidth, lineWidth)

      case _ =>
    }
  })

  // canvas onMouseRelease event
  canvas.setOnMouseReleased((e) => {
    currentTool.value match {
      case "Pen" =>
        gc.lineTo(e.getX(), e.getY());
        gc.stroke();
        gc.closePath();

      case "Eraser" =>
        val lineWidth = gc.getLineWidth();
        gc.fillRect(e.getX() - lineWidth / 2, e.getY() - lineWidth / 2, lineWidth, lineWidth);

      case "Line" =>
        line.setEndX(e.getX)
        line.setEndY(e.getY)
        gc.strokeLine(line.getStartX, line.getStartY, line.getEndX, line.getEndY)
    }
  })

  // tools stuff
  def changeBackgroundColor(canvas: Canvas, color: Color): Unit = {
    // Clear the canvas
      gc.clearRect(0, 0, canvas.width.value, canvas.height.value)


  // Set the new background color
    gc.setFill(color)
    gc.fillRect(0, 0, canvas.width.value, canvas.height.value)
  }

  // clear canvas onAction
  def clearCanvas(actionEvent: ActionEvent): Unit = {
    canvas.graphicsContext2D.clearRect(
      0,
      0,
      canvas.width.value,
      canvas.height.value
    )
    val bgColor = backgroundColorPicker.value.value // Get the selected background color
    changeBackgroundColor(canvas, bgColor)
  }

  // background colour onAction
  def changeBackgroundColour(actionEvent: ActionEvent): Unit = {
    val bgColor = backgroundColorPicker.value.value // Get the selected background color
    changeBackgroundColor(canvas, bgColor)
  }

  // ink colour onAction
  def changeInkColour(actionEvent: ActionEvent): Unit = {
    currentColor = colorPicker.value // Update current color

    // Set the color of the drawing tools based on the selected color
    currentTool.value match {
      case "Pen" | "Pencil" | "Brush" =>
        val context = canvas.graphicsContext2D
        context.setStroke(currentColor.value)
      case _ =>
    }
  }

  // slider on drag done
  widthSlider.valueProperty.addListener((e) => {
    val width = widthSlider.getValue
    gc.setLineWidth(width)
  })

  // tools onAction
  def changeTool(actionEvent: ActionEvent): Unit = {
    currentTool = toolComboBox.value // Update current drawing tool
  }
}