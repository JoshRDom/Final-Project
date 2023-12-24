package hep88.view
import akka.actor.typed.ActorRef
import com.hep88.DrawingClient
import com.hep88.User
import com.hep88.Client
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.control.{ColorPicker, ListView, Slider, TextField}
import javafx.scene.paint.Color
import javafx.scene.shape.{Circle, Line}

import java.awt.event.ActionEvent
import scala.collection.mutable.ListBuffer

@sfxml
class CanvasWindowController(
                            private val canvas: Canvas,
                            private val colorPicker: ColorPicker,
                            private val brushSizeSlider: Slider,
                            private var smallBrush: Circle,
                            private var largeBrush: Circle,
                            private val pencil: Group,
                            private val eraser: Group,
                            private val line: Group,
                            private val fill: Group,
                            private val listMessage: ListView,
                            private val txtMessage: TextField
                            ) {
  var drawingClientRef: Option[ActorRef[DrawingClient.Command]] = None

  val receivedText: ObservableBuffer[String] = new ObservableBuffer[String]()

  // ART STUFF
  // Initialise variables
  // Enum to represent different drawing tools
  private val gc = canvas.getGraphicsContext2D
  private var drawing = false
  private var lines: ListBuffer[Line] = ListBuffer.empty
  private var currentLine: Line = _
  object DrawingTool extends Enumeration {
    val Pencil, Pen, Brush, Eraser = Value
  }
  private var currentTool: DrawingTool.Value = DrawingTool.Pencil
  private var penWidth: Double = brushSizeSlider.getValue
  private var currentColor: Color = colorPicker.getValue

  // COLOR PICKER STUFF
  def setColor(action:ActionEvent): Unit = {
    currentColor = colorPicker.getValue
    smallBrush.setFill( currentColor )
    largeBrush.setFill( currentColor )
  }

  // CANVAS STUFF
  def drawLines(): Unit = {
    gc.clearRect(0, 0, canvas.getWidth, canvas.getHeight)
    setupCanvas()
    gc.setStroke(colorPicker.getValue)
    gc.setLineWidth(2)
    lines.foreach(line => gc.strokeLine(line.getStartX, line.getStartY, line.getEndX, line.getEndY))
  }

  def startDrawing(x: Double, y: Double): Unit = {
    drawing = true
    currentLine = new Line(x, y, x, y)
    lines += currentLine
  }

  def continueDrawing(x: Double, y: Double): Unit = {
    if (drawing) {
      currentLine.setEndX(x)
      currentLine.setEndY(y)
      drawLines()
    }
  }

  def stopDrawing(): Unit = {
    drawing = false
    removeCanvasHandlers()
  }
}
