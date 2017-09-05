package org.ensime.config

import org.ensime.api._
import org.ensime.sexp._
import org.ensime.sexp.formats._
import org.ensime.util.file

import io.scalajs.nodejs.fs._
import io.scalajs.nodejs.path._

import shapeless.cachedImplicit
import slogging._

object EnsimeConfigProtocol extends LazyLogging {
  object Protocol extends DefaultSexpProtocol
    with OptionAltFormat
    with CamelCaseToDashes
  import org.ensime.config.EnsimeConfigProtocol.Protocol._

  implicit object EnsimeFileFormat extends SexpFormat[RawFile] {
    def write(f: RawFile): Sexp = SexpString(f.file.toString)
    def read(sexp: Sexp): RawFile = sexp match {
      case SexpString(file_) => RawFile(file.File(file_))
      case got => deserializationError(got)
    }
  }

  private implicit val projectIdFormat: SexpFormat[EnsimeProjectId] = cachedImplicit
  private implicit val projectFormat: SexpFormat[EnsimeProject] = cachedImplicit
  private implicit val configFormat: SexpFormat[EnsimeConfig] = cachedImplicit

  def parse(config: String): EnsimeConfig = {
    val raw = config.parseSexp.convertTo[EnsimeConfig]
    validated(raw)
  }

  def validated(c: EnsimeConfig): EnsimeConfig = {
    // cats.data.Validated would be a cleaner way to do this
    {
      import c._
      (javaHome :: javaSources ::: javaRunTime(c)).foreach { rawFile =>
        require(Fs.existsSync(rawFile.file.path), s"${rawFile.file.path} is required but does not exist")
      }
    }

    c.copy(projects = c.projects.map(validated))
  }

  def javaRunTime(c: EnsimeConfig): List[RawFile] = treeOf(c.javaHome).filter(rf => Path.basename(rf.file.path) == "rt.jar").toList

  private def treeOf(file: RawFile): List[RawFile] = Nil

  private def validated(p: EnsimeProject): EnsimeProject = {
    (p.targets ++ p.sources).foreach { dir =>
      if (!Fs.existsSync(dir.file.path) && (Path.extname(dir.file.path) != ".jar")) {
        logger.warn(s"$dir does not exist, creating")
        Fs.mkdirSync(dir.file.path)
      }
    }
    p
  }
}
