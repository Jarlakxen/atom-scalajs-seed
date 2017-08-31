// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp

package object formats {
  def deserializationError(got: Sexp, extra: String = "") =
    throw new DeserializationException(s"Unable to parse $got. $extra")

  def serializationError(msg: String) = throw new SerializationException(msg)
}
