// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

import org.ensime.sexp._
import org.scalactic.source.Position

trait FormatSpec extends SexpSpec {
  def assertFormat[T: SexpFormat](start: T, expect: Sexp)(implicit p: Position): Unit = {
    val sexp = start.toSexp
    assert(sexp === expect, s"${sexp.compactPrint} was not ${expect.compactPrint}")
    expect.convertTo[T] should be(start)
  }
}
