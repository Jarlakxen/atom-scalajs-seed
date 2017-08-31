// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

import org.ensime.sexp._

class StandardFormatsSpec extends FormatSpec with StandardFormats with BasicFormats {

  "StandardFormats" should "support Option" in {
    val some = Some("thing")
    assertFormat(some: Option[String], SexpList(SexpString("thing")))
    assertFormat(None: Option[String], SexpNil)
  }

  it should "support Either" in {
    val left = Left(13)
    val right = Right("thirteen")
    assertFormat(
      left: Either[Int, String],
      SexpNumber(13)
    )
    assertFormat(
      right: Either[Int, String],
      SexpString("thirteen")
    )
  }

}
