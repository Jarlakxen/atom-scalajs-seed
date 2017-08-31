// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/gpl-3.0.en.html
package org.ensime.util

import java.util.regex.Pattern
import java.nio.file.Files

import scala.util.Try

import io.scalajs.nodejs.fs._
import io.scalajs.nodejs.path._
import io.scalajs.nodejs.os._

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JavaScriptException

/**
 * Decorate with functionality from common utility
 * packages, which would otherwise be verbose/ugly to call directly.
 *
 */
package object file {
  class File(val path: String, val basename: String) {
    def mkdirs =
      if (!Fs.existsSync(path)) {
        Fs.mkdirSync(path)
      }

    override def toString(): String = path
  }

  def File(path: String): File = new File(path, Path.basename(path))

  def withTempDir[T](a: File => T): T = {
    val dir = Fs.mkdtempSync(s"${OS.tmpdir()}${Path.sep}ensime")
    try a(File(dir))
    finally Try(deleteFolderRecursive(dir))
  }
  /*
  def withTempFile[T](a: File => T): T = {
    val file = Files.createTempFile("ensime-", ".tmp").toFile.canon
    try a(file)
    finally Try(file.delete())
  }*/

  private def deleteFolderRecursive(path: String): Unit = {
    if (Fs.existsSync(path)) {
      Fs.readdirSync(path).toArray.foreach(file => {
        val curPath = path + "/" + file
        if (Fs.lstatSync(curPath).isDirectory()) { // recurse
          deleteFolderRecursive(curPath)
        } else { // delete file
          Fs.unlinkSync(curPath)
        }
      })
      Fs.rmdirSync(path)
    }
  }

  implicit class RichFile(val file: File) extends AnyVal {

    def isScala: Boolean = file.basename.toLowerCase.endsWith(".scala")
    def isJava: Boolean = file.basename.toLowerCase.endsWith(".java")
    def isClassfile: Boolean = file.basename.toLowerCase.endsWith(".class")
    def isJar: Boolean = file.basename.toLowerCase.endsWith(".jar")

    def /(sub: String): File = new File(s"${file.path}${Path.sep}${sub}", sub)

    def exists: Boolean = Fs.existsSync(file.path)
    
    def parts: List[String] =
      file.path.split(
        Pattern.quote(Path.sep)).toList.filterNot(Set("", "."))

    def readLines(): List[String] =
      readString().split(OS.EOL).toList

    def writeLines(lines: List[String]): Unit = {
      writeString(lines.mkString(OS.EOL))
    }

    def writeString(contents: String): Unit =
      Fs.writeFileSync(file.path, contents)

    // TODO: Externalize the encoding
    def readString(): String =
      Fs.readFileSync(file.path, "utf-8")

  }

}