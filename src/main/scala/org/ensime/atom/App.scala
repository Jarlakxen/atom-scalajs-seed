package org.ensime.atom

import scala.scalajs.js
import scala.scalajs.js.annotation._
import facades.atom._
import slogging._

object App extends LazyLogging {

  LoggerConfig.factory = ConsoleLoggerFactory()
  
  @JSExportTopLevel("activate")
  def activate(): Unit = {
    logger.info("Hello World")
    Atom.notifications.addInfo("Hello World")
  }

  @JSExportTopLevel("deactivate")
  def deactivate(): Unit = {

  }

  @JSExportTopLevel("serialize")
  def serialize(): Unit = {

  }
} 
