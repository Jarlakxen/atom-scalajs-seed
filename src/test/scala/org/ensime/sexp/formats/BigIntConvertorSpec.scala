// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

import scala.collection.immutable.BitSet

import BigIntConvertor._
import org.ensime.sexp.SexpSpec
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BigIntConvertorSpec extends SexpSpec {
  private val examples = List(
    BitSet() -> BigInt(0),
    BitSet(0) -> BigInt(1),
    BitSet(1) -> BigInt(2),
    BitSet(64) -> BigInt("18446744073709551616"),
    BitSet(0, 64) -> BigInt("18446744073709551617"),
    BitSet(1, 64) -> BigInt("18446744073709551618")
  )

  "BigIntConvertor" should "convert basic BigSet to BitInt" in {
    examples foreach {
      case (bitset, bigint) => fromBitSet(bitset) should ===(bigint)
    }
  }

  it should "convert basic BigInt to BitSet" in {
    examples foreach {
      case (bitset, bigint) => toBitSet(bigint) should ===(bitset)
    }
  }
}

class BigIntConvertorCheck extends SexpSpec with GeneratorDrivenPropertyChecks {

  def positiveIntStream: Arbitrary[Stream[Int]] = Arbitrary {
    Gen.containerOf[Stream, Int](Gen.chooseNum(0, 2 * Short.MaxValue))
  }

  implicit def arbitraryBitSet: Arbitrary[BitSet] = Arbitrary {
    for (seq <- positiveIntStream.arbitrary) yield BitSet(seq: _*)
  }

  it should "round-trip BitSet <=> BitSet" in {
    // NOTE: roundtripping BigInt <=> BigInt is not required
    //       https://issues.scala-lang.org/browse/SI-10162
    forAll { (bitset: BitSet) =>
      toBitSet(fromBitSet(bitset)) should ===(bitset)
    }
  }
}
