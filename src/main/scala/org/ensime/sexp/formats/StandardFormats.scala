package org.ensime.sexp.formats

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.UUID

import org.ensime.sexp._
import org.ensime.util.file
import org.ensime.util.file.File

/**
 * Formats for data types that are so popular that you'd expect them
 * to "just work".
 *
 * Most people might expect `Option[T]` to output `nil` for `None` and
 * the instance for `Some`, but that doesn't round-trip for nested
 * types (think of `Option[List[T]]`). Instead we use a one-element
 * list. If you want to have the non-round-trip behaviour, mix in
 * `OptionAltFormat`.
 */
trait StandardFormats {
  implicit def optionFormat[T: SexpFormat]: SexpFormat[Option[T]] = new SexpFormat[Option[T]] {
    def write(option: Option[T]) = option match {
      case Some(x) => SexpList(x.toSexp)
      case None => SexpNil
    }
    def read(value: Sexp) = value match {
      case SexpNil => None
      case SexpList(s) => Some(s.head.convertTo[T])
      case x => deserializationError(x)
    }
  }

  import scala.util.Success
  import scala.util.Failure
  import SexpFormatUtils._
  implicit def eitherFormat[L: SexpFormat, R: SexpFormat]: SexpFormat[Either[L, R]] =
    new SexpFormat[Either[L, R]] {
      def write(either: Either[L, R]) = either match {
        case Left(b) => b.toSexp
        case Right(a) => a.toSexp
      }
      def read(value: Sexp) = (value.convertTo(safeReader[L]), value.convertTo(safeReader[R])) match {
        case (Success(l), Failure(_)) => Left(l)
        case (Failure(l), Success(r)) => Right(r)
        case (_, _) => deserializationError(value)
      }
    }

  trait ViaString[T] {
    def toSexpString(t: T): String
    def fromSexpString(s: String): T
  }
  def viaString[T](via: ViaString[T]): SexpFormat[T] = new SexpFormat[T] {
    def write(t: T): Sexp = SexpString(via.toSexpString(t))
    def read(v: Sexp): T = v match {
      case SexpString(s) => via.fromSexpString(s)
      case x => deserializationError(x)
    }
  }

  implicit val UuidFormat: SexpFormat[UUID] = viaString(new ViaString[UUID] {
    def toSexpString(uuid: UUID) = uuid.toString
    def fromSexpString(s: String) = UUID.fromString(s)
  })


  implicit val FileFormat: SexpFormat[File] = viaString(new ViaString[File] {
    def toSexpString(file_ : File) = file_.path
    def fromSexpString(s: String) = file.File(s)
  })
}

trait OptionAltFormat {
  this: StandardFormats =>

  override implicit def optionFormat[T: SexpFormat]: SexpFormat[Option[T]] =
    new SexpFormat[Option[T]] {
      def write(option: Option[T]) = option match {
        case Some(x) => x.toSexp
        case None => SexpNil
      }
      def read(value: Sexp) = value match {
        case SexpNil => None
        case x => Some(x.convertTo[T])
      }
    }

}
