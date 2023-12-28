package com.hep88.view
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent,MouseDragEvent}
import scalafx.scene.control.{ColorPicker, ComboBox, ListView, Slider, TextField}
import com.hep88.DrawingBoardClient
import com.hep88.User
import com.hep88.DrawingBoardApp
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color

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
  val initialContext = canvas.getGraphicsContext2D()

  // VARIABLE INITIALISATIONS
  // chat stuff
  listMessage.items = receivedText

  // canvas stuff
  initialContext.setFill(Color.White) // Set your initial canvas color here
  initialContext.fillRect(0, 0, canvas.width.value, canvas.height.value)

  // tools stuff
  colorPicker.value = Color.Black
  var currentColor = colorPicker.value
  toolComboBox.items = ObservableBuffer[String]("Pen", "Pencil", "Brush", "Eraser")
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
  def startDrawing(event: MouseEvent): Unit = {
    val context = canvas.graphicsContext2D
    context.beginPath()
    context.moveTo(event.x, event.y)
    context.stroke()
  }

  // canvas onMouseDrag event
  def continueDrawing(event: MouseDragEvent): Unit = {
    val context = canvas.graphicsContext2D

    // Choose drawing tool based on the current selection
    currentTool.value match {

      case "Pen" =>
        context.lineTo(event.x, event.y)
        context.setStroke(currentColor.value)
        context.stroke()

      case "Pencil" =>
        context.beginPath()
        context.moveTo(event.x, event.y)
        context.lineTo(event.x + 1, event.y + 1) // Example of pencil drawing
        context.setStroke(currentColor.value)
        context.stroke()

      case "Brush" =>
        // Customize brush behavior
        // Implement brush logic here
        // Example: Vary stroke width based on the slider value
        val strokeWidth = widthSlider.value
        context.setLineWidth(strokeWidth.getValue * 2)
        context.lineTo(event.x, event.y)
        context.setStroke(currentColor.value)
        context.stroke()

      case "Eraser" =>
        val bgColor = backgroundColorPicker.value.value // Get the selected background color
        val eraserSize = widthSlider.value.value // Get the actual value from the binding
        val eraserHalfSize = eraserSize / 2

        context.setFill(bgColor)
        context.fillRect(event.x - eraserHalfSize, event.y - eraserHalfSize, eraserSize, eraserSize)

    }
  }

  // tools stuff
  def changeBackgroundColor(canvas: Canvas, color: Color): Unit = {
    val context = canvas.graphicsContext2D

    // Clear the canvas
    context.clearRect(0, 0, canvas.width.value, canvas.height.value)

    // Set the new background color
    context.setFill(color)
    context.fillRect(0, 0, canvas.width.value, canvas.height.value)
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

  // tools onAction
  def changeTool(actionEvent: ActionEvent): Unit = {
    currentTool = toolComboBox.value // Update current drawing tool
  }
}