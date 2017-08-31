package org.ensime.config

import org.ensime.api._
import org.ensime.util.file._

package object richconfig {

  implicit class RichEnsimeConfig(val c: EnsimeConfig) extends AnyVal {
    def lookup(id: EnsimeProjectId) = c.projects.find(_.id == id).get
  }

  implicit class RichEnsimeProject(val p: EnsimeProject) extends AnyVal {
    def dependencies(implicit config: EnsimeConfig): List[EnsimeProject] =
      p.depends.map(config.lookup)
  }

}