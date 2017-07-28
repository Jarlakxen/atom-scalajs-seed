package facades

import scala.scalajs.js
import scala.scalajs.js.annotation._

object atom {

  @js.native
  trait NewTile extends js.Object {
    val item: Any = js.native
    val priority: Int = js.native
  }

  @js.native
  trait Tile extends js.Object {
    def getPriority(): Int = js.native
    def getItem(): Any = js.native
    def destroy(): Unit = js.native
  }

  @js.native
  trait StatusBar extends js.Object {
    def addLeftTile(tile: NewTile): Unit = js.native
    def addRightTile(tile: NewTile): Unit = js.native
    def getLeftTiles(): List[Tile] = js.native
    def getRightTiles(): List[Tile] = js.native
  }

  @js.native
  trait INotifications extends js.Object {
    def addInfo(msg: String): Unit = js.native
    def addError(msg: String): Unit = js.native
    def addSuccess(msg: String): Unit = js.native
    def addWarning(msg: String): Unit = js.native
  }

  @js.native
  @JSGlobal("atom")
  object Atom extends js.Object {
    val notifications: INotifications = js.native
  }

  trait AtomPlugin {
    def activate(): Unit
    def deactivate(): Unit
    def serialize(): Unit
  }

}