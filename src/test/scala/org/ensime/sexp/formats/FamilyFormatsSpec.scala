// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

import java.util.UUID

import org.ensime.sexp._
import shapeless._

// Example domain models used in the tests. Note that the domain model
// and formatters are defined in sibling packages.
package examples {
  sealed trait SimpleTrait
  case class Foo(s: String) extends SimpleTrait
  case class Bar() extends SimpleTrait
  case object Baz extends SimpleTrait
  case class Faz(o: Option[String]) extends SimpleTrait

  sealed trait SubTrait extends SimpleTrait
  case object Fuzz extends SubTrait

  sealed trait Spiel
  case object Buzz extends Spiel

  case class Schpugel(v: String) // I asked my wife to make up a word
  case class Smim(v: String) // I should stop asking my wife to make up words

  sealed trait Cloda
  case class Plooba(thing: String) extends Cloda // *sigh*

  object Quack
  case class Huey(duck: Quack.type, witch: Option[Quack.type])
  case class Dewey(duck: Quack.type, witch: Option[Quack.type])

  // I love monkeys, you got a problem with that?
  sealed trait Primates
  sealed trait Strepsirrhini extends Primates
  sealed trait Haplorhini extends Primates
  sealed trait Tarsiiformes extends Haplorhini
  case object Tarsiidae extends Tarsiiformes
  sealed trait Simiiformes extends Haplorhini
  sealed trait Platyrrhini extends Simiiformes
  case object Callitrichidae extends Platyrrhini
  case object Cebidae extends Platyrrhini
  case object Aotidae extends Platyrrhini
  case object Pitheciidae extends Platyrrhini
  case object Atelidae extends Platyrrhini
  sealed trait Catarrhini extends Simiiformes
  sealed trait Cercopithecoidea extends Catarrhini
  case object Cercopithecidae extends Cercopithecoidea
  sealed trait Hominoidea extends Catarrhini
  case object Hylobatidae extends Hominoidea
  case class Hominidae(id: UUID) extends Hominoidea

  // recursive cat
  case class Cat(nick: String, tail: Option[Cat] = None)
}

package formats {
  object ExamplesFormats extends BasicFormats
      with StandardFormats
      with CollectionFormats
      with SymbolAltFormat
      with OptionAltFormat
      with FamilyFormats {
    import examples._

    ///////////////////////////////////////////////
    // Example of "explicit implicit" for performance
    implicit val SimpleTraitFormat: SexpFormat[SimpleTrait] = cachedImplicit

    ///////////////////////////////////////////////
    // user-defined hinting
    implicit object SubTraitHint extends FlatCoproductHint[SubTrait]
    implicit object SpielHint extends FlatCoproductHint[Spiel](SexpSymbol(":hint"))

    ///////////////////////////////////////////////
    // user-defined field naming rules
    implicit object ClodaHint extends FlatCoproductHint[Cloda](SexpSymbol(":TYPE")) {
      override def field(orig: String): SexpSymbol = SexpSymbol(orig.toUpperCase)
    }
    implicit object PloobaHint extends BasicProductHint[Plooba] {
      override def field[K <: Symbol](k: K): SexpSymbol = SexpSymbol(s":${k.name.toUpperCase}")
    }

    ///////////////////////////////////////////////
    // user-defined /missing value rules
    implicit object DeweyHint extends BasicProductHint[Dewey] {
      override def nulls = AlwaysSexpNil
    }
    implicit object QuackFormat extends SexpFormat[Quack.type] {
      // needed something that would serialise to nil for testing
      def read(j: Sexp): Quack.type = j match {
        case SexpNil => Quack
        case other => deserializationError(other)
      }
      def write(q: Quack.type): Sexp = SexpNil
    }

    ///////////////////////////////////////////////
    // user-defined SexpFormat
    implicit object SchpugelFormat extends SexpFormat[Schpugel] {
      def read(j: Sexp): Schpugel = j match {
        case SexpString(v) => Schpugel(v)
        case other => deserializationError(other)
      }
      def write(s: Schpugel): Sexp = SexpString(s.v)
    }

  }
}

package test {
  class FamilyFormatsSpec extends FormatSpec {
    import examples._
    import formats.ExamplesFormats._

    def roundtrip[T: SexpFormat](t: T, wire: String): Unit =
      assertFormat(t, wire.parseSexp)

    "FamilyFormats" should "support case objects" in {
      roundtrip(Baz, "()")
    }

    it should "support symbols" in {
      roundtrip('foo, """foo""")
    }

    it should "support case classes" in {
      roundtrip(Foo("foo"), """(:s "foo")""")
      roundtrip(Bar(), "()")
    }

    it should "support recursive case classes" in {
      roundtrip(
        Cat(
          "foo",
          Some(Cat(
            "bar",
            Some(Cat("baz"))
          ))
        ),
        """(:nick "foo" :tail (:nick "bar" :tail (:nick "baz")))"""
      )
    }

    it should "support optional parameters on case classes" in {
      roundtrip(Faz(Some("meh")), """(:o "meh")""")
      roundtrip(Faz(None), "()")
    }

    it should "fail when missing required fields" in {
      intercept[DeserializationException] {
        """()""".parseSexp.convertTo[Foo]
      }
    }

    it should "support simple sealed families" in {
      roundtrip(Foo("foo"): SimpleTrait, """(:Foo (:s "foo"))""")
      roundtrip(Bar(): SimpleTrait, """:Bar""")
      roundtrip(Baz: SimpleTrait, """:Baz""")
      roundtrip(Fuzz: SimpleTrait, """:Fuzz""")
    }

    it should "fail when missing required coproduct disambiguators" in {
      intercept[DeserializationException] {
        """(:s "foo")""".parseSexp.convertTo[SimpleTrait]
      }
    }

    it should "support custom coproduct keys" in {
      roundtrip(Fuzz: SubTrait, """(:type Fuzz)""")
      roundtrip(Buzz: Spiel, """(:hint Buzz)""")
    }

    it should "support custom coproduct field naming rules" in {
      roundtrip(Plooba("poo"): Cloda, """(:TYPE PLOOBA :THING "poo")""")
    }

    it should "support custom product field naming rules" in {
      roundtrip(Plooba("poo"), """(:THING "poo")""")
    }

    it should "support custom missing value rules" in {
      roundtrip(Huey(Quack, None), """nil""")
      roundtrip(Dewey(Quack, None), """(:duck nil :witch nil)""")

      val nulls = """(:duck nil :witch nil)""".parseSexp
      nulls.convertTo[Huey] shouldBe Huey(Quack, None)
      nulls.convertTo[Dewey] shouldBe Dewey(Quack, None)

      val partial = """(:duck nil)""".parseSexp
      partial.convertTo[Huey] shouldBe Huey(Quack, None)
      intercept[DeserializationException] {
        partial.convertTo[Dewey] shouldBe Dewey(Quack, None)
      }

      val empty = """()""".parseSexp
      empty.convertTo[Huey] shouldBe Huey(Quack, None)
      intercept[DeserializationException] {
        empty.convertTo[Dewey] shouldBe Dewey(Quack, None)
      }
    }

    it should "fail when missing required (null) values" in {
      val noduck = """(:witch nil)""".parseSexp
      val nowitch = """(:duck nil)""".parseSexp

      noduck.convertTo[Huey]
      intercept[DeserializationException] {
        noduck.convertTo[Dewey]
      }

      nowitch.convertTo[Huey]
      intercept[DeserializationException] {
        nowitch.convertTo[Dewey] shouldBe Dewey(Quack, None)
      }
    }

    it should "prefer user customisable SexpFormats" in {
      roundtrip(Schpugel("foo"), """"foo"""")
    }

    it should "fail to compile when a member of the family cannot be serialised" in {
      // remove UUID formatter
      def UuidFormat: SexpFormat[UUID] = ???

      shapeless.test.illTyped(
        """roundtrip(Hominidae(UUID.randomUUID): Primates, "nil")""",
        ".*Cannot find SexpFormat for org.ensime.sexp.formats.examples.Primates.*"
      )

      shapeless.test.illTyped(
        """roundtrip(Hominidae(UUID.randomUUID), "nil")""",
        ".*Cannot find SexpFormat for org.ensime.sexp.formats.examples.Hominidae.*"
      )
    }

    ///////////////////////////////////////////////
    // non-trivial AST (in separate file)
    it should "support an example ADT" in {
      import ExampleAst._

      roundtrip(SpecialToken: TokenTree, """:SpecialToken""")

      val fieldTerm = FieldTerm("thing is ten", DatabaseField("THING"), "10")
      roundtrip(
        fieldTerm: TokenTree,
        """(:FieldTerm (:text "thing is ten" :field (:column "THING") :value "10"))"""
      )

      val and = AndCondition(fieldTerm, fieldTerm, "wibble")
      roundtrip(
        and: TokenTree,
        """(:AndCondition (:left (:FieldTerm (:text "thing is ten" :field (:column "THING") :value "10")) :right (:FieldTerm (:text "thing is ten" :field (:column "THING") :value "10")) :text "wibble"))"""
      )
    }
  }
}
