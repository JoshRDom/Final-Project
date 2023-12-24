package hep88

package com.hep88
import akka.cluster.typed._
import akka.{ actor => classic }
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.ConfigFactory
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scala.concurrent.Future
import scala.concurrent.duration._


object Client extends JFXApp {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val greeterMain: ActorSystem[DrawingClient.Command] = ActorSystem(DrawingClient(), "HelloSystem")

  greeterMain ! DrawingClient.start


  val loader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(getClass.getResourceAsStream("view/MainWindow.fxml")) // insert main screen
  val border: scalafx.scene.layout.BorderPane = loader.getRoot[javafx.scene.layout.BorderPane]()
  val control = loader.getController[com.hep88.view.CanvasWindowController#Controller]() // insert main screen controller
  control.drawingClientRef = Option(greeterMain) // wire to controller
  stage = new PrimaryStage() {
    scene = new Scene(){
      root = border
    }
  }

  stage.onCloseRequest = handle( {
    greeterMain.terminate
  })
}
