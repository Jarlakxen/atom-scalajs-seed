// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

import scala.collection.immutable.ListMap

import org.ensime.sexp._
import shapeless._, labelled.{ field, FieldType }
import slogging._
/**
 * Automatically create product/coproduct marshallers (i.e. families
 * of sealed traits and case classes/objects) for s-express.
 *
 * This uses s-expression data as the underlying format, as opposed to
 * alists. Alists are arguably a better wire format because they allow
 * for arbitrarily complex keys, but we're applying the Principle of
 * Least Power.
 *
 * Based on spray-json-shapeless, with the same caveats.
 */
trait FamilyFormats extends LowPriorityFamilyFormats {
  this: StandardFormats =>
}

private[formats] trait LowPriorityFamilyFormats
    extends SexpFormatHints
    with LazyLogging {
  this: StandardFormats with FamilyFormats =>

  /**
   * a `SexpFormat[HList]` or `SexpFormat[Coproduct]` would not retain the
   * type information for the full generic that it is serialising.
   * This allows us to pass the wrapped type, achieving: 1) custom
   * `CoproductHint`s on a per-trait level 2) configurable `null` behaviour
   * on a per product level 3) clearer error messages.
   *
   * This is intentionally not an `SexpFormat` to avoid ambiguous
   * implicit errors, even though it implements its interface.
   */
  abstract class WrappedSexpFormat[Wrapped, SubRepr] {
    final def read(s: Sexp): SubRepr = s match {
      case SexpNil => readData(ListMap.empty)
      case key: SexpSymbol => readData(ListMap(key -> SexpNil))
      case SexpData(data) => readData(data)
      case other => deserializationError(other)
    }
    def readData(j: SexpData): SubRepr

    final def write(r: SubRepr): Sexp = writeData(r).toSeq match {
      case Seq((key, SexpNil)) => key
      case data => squash(data)
    }

    def writeData(r: SubRepr): SexpData
  }

  implicit def hNilFormat[Wrapped]: WrappedSexpFormat[Wrapped, HNil] = new WrappedSexpFormat[Wrapped, HNil] {
    override def readData(s: SexpData) = HNil
    override def writeData(n: HNil) = ListMap.empty
  }

  implicit def hListFormat[Wrapped, Key <: Symbol, Value, Remaining <: HList](
    implicit
    ph: ProductHint[Wrapped],
    key: Witness.Aux[Key],
    sfh: Lazy[SexpFormat[Value]],
    sft: WrappedSexpFormat[Wrapped, Remaining]
  ): WrappedSexpFormat[Wrapped, FieldType[Key, Value] :: Remaining] =
    new WrappedSexpFormat[Wrapped, FieldType[Key, Value] :: Remaining] {
      private[this] val fieldName: SexpSymbol = ph.field(key.value)

      override def readData(s: SexpData) = {
        val resolved: Value = s.get(fieldName) match {
          case None if ph.nulls == AlwaysSexpNil =>
            val found = s.map(_._1.value)
            throw new DeserializationException(s"missing ${fieldName.value}, found ${found.mkString(",")}")

          case value => sfh.value.read(value.getOrElse(SexpNil))
        }
        val remaining = sft.readData(s)
        field[Key](resolved) :: remaining
      }

      override def writeData(ft: FieldType[Key, Value] :: Remaining) = sfh.value.write(ft.head) match {
        case SexpNil if ph.nulls == NeverSexpNil => sft.writeData(ft.tail)
        case value => ListMap(fieldName -> value) ++: sft.writeData(ft.tail)
      }
    }

  implicit def cNilFormat[Wrapped]: WrappedSexpFormat[Wrapped, CNil] = new WrappedSexpFormat[Wrapped, CNil] {
    override def readData(s: SexpData) =
      throw new DeserializationException(s"read should never be called for CNil, $s")
    override def writeData(c: CNil) =
      throw new DeserializationException("write should never be called for CNil")
  }

  implicit def coproductFormat[Wrapped, Name <: Symbol, Instance, Remaining <: Coproduct](
    implicit
    th: CoproductHint[Wrapped],
    key: Witness.Aux[Name],
    sfh: Lazy[SexpFormat[Instance]],
    sft: WrappedSexpFormat[Wrapped, Remaining]
  ): WrappedSexpFormat[Wrapped, FieldType[Name, Instance] :+: Remaining] =
    new WrappedSexpFormat[Wrapped, FieldType[Name, Instance] :+: Remaining] {

      override def readData(s: SexpData) = th.read(s, key.value) match {
        case Some(product) =>
          val recovered = sfh.value.read(squash(product))
          Inl(field[Name](recovered))

        case None =>
          Inr(sft.readData(s))
      }

      override def writeData(lr: FieldType[Name, Instance] :+: Remaining) = lr match {
        case Inl(l) => sfh.value.write(l) match {
          case SexpNil => th.write(ListMap.empty, key.value)
          case SexpData(data) => th.write(data, key.value)
          case other => serializationError(s"expected SexpData, got $other")
        }

        case Inr(r) => sft.writeData(r)
      }

    }

  /**
   * Format for `LabelledGenerics` that uses the `HList` or `Coproduct`
   * marshaller above.
   *
   * `Blah.Aux[T, Repr]` is a trick to work around scala compiler
   * constraints. We'd really like to have only one type parameter
   * (`T`) implicit list `g: LabelledGeneric[T], f:
   * Cached[Strict[SexpFormat[T.Repr]]]` but that's not possible.
   */
  implicit def familyFormat[T, Repr](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    sg: Cached[Strict[WrappedSexpFormat[T, Repr]]],
    tpe: Typeable[T]
  ): SexpFormat[T] = new SexpFormat[T] {
    logger.trace(s"creating ${tpe.describe}")

    def read(s: Sexp): T = gen.from(sg.value.value.read(s))
    def write(t: T): Sexp = sg.value.value.write(gen.to(t))
  }
}

trait SexpFormatHints {
  private[formats] def squash(d: Seq[(SexpSymbol, Sexp)]): Sexp =
    d.foldRight(SexpNil: Sexp) {
      case ((key, value), acc) => SexpCons(key, SexpCons(value, acc))
    }

  private[formats] def squash(d: SexpData): Sexp = squash(d.toSeq)

  trait CoproductHint[T] {
    /**
     * Given the `SexpData` for the sealed family, disambiguate and
     * extract the `SexpData` associated to the `Name` implementation
     * (if available) or otherwise return `None`.
     */
    def read[Name <: Symbol](s: SexpData, n: Name): Option[SexpData]

    /**
     * Given the `SexpData` for the contained product type of `Name`,
     * encode disambiguation information for later retrieval.
     */
    def write[Name <: Symbol](s: SexpData, n: Name): SexpData

    /**
     * Override to provide custom field naming.
     * Caching is recommended for performance.
     */
    protected def field(orig: String): SexpSymbol
  }

  /**
   * Product types are disambiguated by a `(:key value ...)`. Of
   * course, this will fail if the product type has a field with the
   * same name as the key. The default key is the word "type" which is
   * a keyword in Scala so unlikely to collide with too many case
   * classes.
   */
  class FlatCoproductHint[T: Typeable](key: SexpSymbol = SexpSymbol(":type")) extends CoproductHint[T] {
    override def field(orig: String): SexpSymbol = SexpSymbol(orig)

    def read[Name <: Symbol](d: SexpData, n: Name): Option[SexpData] = {
      if (d.get(key) == Some(field(n.name))) Some(d)
      else None
    }
    def write[Name <: Symbol](d: SexpData, n: Name): SexpData = {
      ListMap(key -> field(n.name)) ++: d
    }
  }

  /**
   * Product types are disambiguated by an extra layer containing a
   * single key which is the name of the type of product contained in
   * the value. e.g. `(:my-type (...))`
   */
  class NestedCoproductHint[T: Typeable] extends CoproductHint[T] {
    override def field(orig: String): SexpSymbol = SexpSymbol(s":${orig}")

    def read[Name <: Symbol](d: SexpData, n: Name): Option[SexpData] = {
      d.get(field(n.name)).collect {
        case SexpNil => ListMap.empty
        case SexpData(data) => data
      }
    }
    def write[Name <: Symbol](d: SexpData, n: Name): SexpData = {
      ListMap(field(n.name) -> squash(d))
    }
  }

  implicit def coproductHint[T: Typeable]: CoproductHint[T] = new NestedCoproductHint[T]

  /**
   * Sometimes the wire format needs to match an existing format and
   * `SexpNil` behaviour needs to be customised. This allows null
   * behaviour to be defined at the product level. Field level control
   * is only possible with a user-defined `SexpFormat`.
   */
  sealed trait SexpNilBehaviour
  /** All values serialising to `SexpNil` will be included in the wire format. */
  case object AlwaysSexpNil extends SexpNilBehaviour
  /** No values serialising to `SexpNil` will be included in the wire format. */
  case object NeverSexpNil extends SexpNilBehaviour

  trait ProductHint[T] {
    def nulls: SexpNilBehaviour
    def field[Key <: Symbol](key: Key): SexpSymbol
  }
  class BasicProductHint[T] extends ProductHint[T] {
    override def nulls: SexpNilBehaviour = NeverSexpNil
    override def field[Key <: Symbol](key: Key): SexpSymbol = SexpSymbol(s":${key.name}")
  }

  implicit def productHint[T]: ProductHint[T] = new BasicProductHint
}
