// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/gpl-3.0.en.html
package org.ensime.util

import java.util.HashMap

import org.ensime.api._
import org.ensime.util.file._

/**
 * Adds functionality to the EnsimeFile sealed family, without
 * polluting the API with implementation detail.
 */
package ensimefile {

  trait RichEnsimeFile {
    def isJava: Boolean
    def isJar: Boolean
    def isScala: Boolean
    def exists(): Boolean

    /** Direct access contents: not efficient for streaming. */
    def readStringDirect(): String
    def readAllLines: List[String]
  }

}

package object ensimefile {

  private val ArchiveRegex = "(?:(?:jar:)?file:)?([^!]++)!(.++)".r
  private val FileRegex = "(?:(?:jar:)?file:)?(.++)".r
  def EnsimeFile(path: String): EnsimeFile = path match {
    case ArchiveRegex(file, entry) => ArchiveFile(File(file), entry)
    case FileRegex(file) => RawFile(File(file))
  }

  // URIs on Windows can look like /C:/path/to/file, which are malformed
  private val BadWindowsRegex = "/+([^:]+:[^:]+)".r
  private def cleanBadWindows(file: String): String = file match {
    case BadWindowsRegex(clean) => clean
    case other => other
  }

  implicit class RichRawFile(val raw: RawFile) extends RichEnsimeFile {
    // PathMatcher is too complex, use http://stackoverflow.com/questions/20531247
    override def isJava: Boolean = raw.file.path.toString.toLowerCase.endsWith(".java")
    override def isJar: Boolean = raw.file.path.toString.toLowerCase.endsWith(".jar")
    override def isScala: Boolean = raw.file.path.toString.toLowerCase.endsWith(".scala")
    override def exists(): Boolean = raw.file.exists
    override def readStringDirect(): String = raw.file.readString()
    override def readAllLines: List[String] = raw.file.readLines()
  }

  // most methods require obtaining the Path of the entry, within the
  // context of the archive file, and ensuring that we close the
  // resource afterwards (which is slow for random access)
  implicit class RichArchiveFile(val archive: ArchiveFile) extends RichEnsimeFile {
    override def isJava: Boolean = archive.entry.toLowerCase.endsWith(".java")
    override def isJar: Boolean = archive.entry.toLowerCase.endsWith(".jar")
    override def isScala: Boolean = archive.entry.toLowerCase.endsWith(".scala")
    override def exists(): Boolean = archive.jar.exists
    override def readStringDirect(): String = ""
    override def readAllLines: List[String] = Nil

    def readBytes(): Array[Byte] = Array.emptyByteArray

    def fullPath: String = s"${archive.jar}!${archive.entry}"
  }

  // boilerplate-tastic... Coproduct would be helpful here
  implicit def richEnsimeFile(ensime: EnsimeFile): RichEnsimeFile = ensime match {
    case raw: RawFile => new RichRawFile(raw)
    case archive: ArchiveFile => new RichArchiveFile(archive)
  }
}

