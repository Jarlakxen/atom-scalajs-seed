// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime

import scala.collection.immutable.ListMap

package sexp {
  class DeserializationException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)
  class SerializationException(msg: String) extends RuntimeException(msg)

}

package object sexp {
  type SexpData = ListMap[SexpSymbol, Sexp]

  implicit class EnrichedAny[T](val any: T) extends AnyVal {
    def toSexp(implicit writer: SexpWriter[T]): Sexp = writer.write(any)
  }

  implicit class EnrichedString(val string: String) extends AnyVal {
    def parseSexp: Sexp = SexpParser.parse(string)
  }
}
