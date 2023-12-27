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


object DrawingBoardApp extends JFXApp {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val greeterMain: ActorSystem[DrawingBoardClient.Command] = ActorSystem(DrawingBoardClient(), "DrawingBoardSystem")

  greeterMain ! DrawingBoardClient.start


  val loader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(getClass.getResourceAsStream("view/DrawingBoardView.fxml"))
  val border: scalafx.scene.layout.BorderPane = loader.getRoot[javafx.scene.layout.BorderPane]()
  val control = loader.getController[com.hep88.view.DrawingBoardController#Controller]()
  control.drawingBoardClientRef = Option(greeterMain)
  stage = new PrimaryStage() {
    title = "Drawing Board App"
    scene = new Scene(){
      root = border
    }
  }

  stage.onCloseRequest = handle( {
    greeterMain.terminate
  })


}