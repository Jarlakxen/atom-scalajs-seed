package org.ensime.util

import file._

class EscapingStringInterpolationSpec extends EnsimeSpec {

  import EscapingStringInterpolation._

  "EscapingStringInterpolation" should "hijack File" in {
    val f = File("""C:\""")
    s"$f" shouldBe """C:\\"""
  }

  it should "not affect normal interpolation" in {
    s"nothing here" shouldBe "nothing here"

    val thing = "foo"
    s"${1 + 2} $thing" shouldBe "3 foo"
  }

}