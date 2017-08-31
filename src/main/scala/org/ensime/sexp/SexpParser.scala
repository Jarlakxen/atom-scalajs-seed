// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp

//import org.parboiled2._
import fastparse.all._

/**
 * Parse Emacs Lisp into an `Sexp`. Other lisp variants may
 * require tweaking, e.g. Scheme's nil, infinity, NaN, etc.
 */
object SexpParser {

  def parse(desc: String): Sexp = {
    SexpP.parse(desc) match {
      case Parsed.Success(d, _) => d
      case f: Parsed.Failure =>
        throw new Exception("Failed to parse sexp: " + f.msg)
    }
  }

  // https://www.gnu.org/software/emacs/manual/html_node/elisp/Basic-Char-Syntax.html
  // https://www.gnu.org/software/emacs/manual/html_node/elisp/Syntax-for-Strings.html
  // Not supported: https://www.gnu.org/software/emacs/manual/html_node/elisp/Non_002dASCII-in-Strings.html
  private[sexp] val specialChars = Map[String, String](
    "\"" -> "\"",
    "a" -> "\u0007",
    "b" -> "\b",
    "t" -> "\t",
    "n" -> "\n",
    "v" -> "\u000b",
    "f" -> "\f",
    "r" -> "\r",
    "e" -> "\u001b",
    "s" -> " ",
    "d" -> "\u007f",
    "\\" -> "\\"
  )

  val SexpQuote = SexpSymbol("quote")

  val PrintableRange = '\u0021' to '\u007e'
  val Symbols = "+-*/_~!@$%^&=:<>{}"
  val SymbolStartChar = Seq[Seq[Char]]('0' to '9', 'a' to 'z', 'A' to 'Z', Symbols)

  val NormalCharPredicate = CharPred(c => PrintableRange.contains(c) && "\"\\".forall(_ != c))
  val WhiteSpacePredicate = CharIn(" \n\r\t\f")
  val NotNewLinePredicate = CharPred(c => PrintableRange.contains(c) && c != '\n')
  val SymbolStartCharPredicate = CharIn(SymbolStartChar: _*)
  val SymbolBodyCharPredicate = CharIn(SymbolStartChar :+ ".".toSeq: _*)
  val PlusMinusPredicate = CharIn("+-")
  val ExpPredicate = CharIn("eE")
  val QuoteSlashBackSlash = CharIn("\"\\/")
  val NCCharPredicate = CharPred(c => "\"\\".forall(_ != c))

  private val SexpP: Parser[Sexp] =
    P(SexpAtomP | SexpListP | SexpEmptyList | SexpConsP | SexpQuotedP)

  private val SexpConsP: Parser[SexpCons] =
    P(LeftBrace ~ SexpP ~ Whitespace ~ "." ~ Whitespace ~ SexpP ~ RightBrace)
      .map(SexpCons.tupled)

  private val SexpListP: Parser[Sexp] =
    P(LeftBrace ~ SexpP ~ (Whitespace ~ SexpP).rep ~ RightBrace).map {
      case (head, tail) => SexpList(head :: tail.toList)
    }

  private val SexpAtomP: Parser[SexpAtom] =
    P(SexpCharP | SexpStringP | SexpNaNP | SexpNumberP | SexpSymbolP)

  private val SexpCharP: Parser[SexpChar] =
    P("?" ~ NormalChar).map(SexpChar)

  val SexpStringP: Parser[SexpString] = P("\"" ~ Characters ~ "\"").map(SexpString)

  val Characters: Parser[String] = P((NormalCharS | EscapedCharS).rep
    .map(chars => chars.mkString))

  val NormalCharS: P[String] = P(NCCharPredicate.!)

  val EscapedCharS: P[String] =
    P("\\" ~
      (QuoteSlashBackSlash.!
        | CharIn("\"").map(_ => "\"")
        | CharIn("b").map(_ => "\b")
        | CharIn("s").map(_ => " ")
        | CharIn("f").map(_ => "\f")
        | CharIn("n").map(_ => "\n")
        | CharIn("r").map(_ => "\r")
        | CharIn("t").map(_ => "\t")
        | CharIn(" \n").map(_ => "") // special emacs magic for comments \<space< and \<newline> are removed
        | CharIn("a").map(_ => "\u0007") // bell
        | CharIn("v").map(_ => "\u000b") // vertical tab
        | CharIn("e").map(_ => "\u001b") // escape
        | CharIn("d").map(_ => "\u007f")) // DEL
    )

  val SexpNumberP = P((Integer ~ Frac.? ~ Exp.?).!)
    .map(num => SexpNumber(BigDecimal(num)))

  val Integer = P("-".? ~ ((CharIn('1' to '9') ~ Digits) | CharIn('0' to '9')))

  val Digits = P(CharIn('0' to '9').rep(1))

  val Frac = P("." ~ Digits)

  val Exp = P(ExpPredicate ~ PlusMinusPredicate.? ~ Digits)

  private val SexpNaNP: Parser[SexpAtom] =
    P(StringIn("-1.0e+INF").map(_ => SexpNegInf) |
      StringIn("1.0e+INF").map(_ => SexpPosInf) |
      ("-".? ~ "0.0e+NaN").map(_ => SexpNaN))

  private val SexpQuotedP: Parser[Sexp] =
    P("\'" ~ SexpP).map(SexpCons(SexpQuote, _))

  private val SexpSymbolP: Parser[SexpAtom] =
    // ? allowed at the end of symbol names
    P((SymbolStartCharPredicate.rep(1) ~ SymbolBodyCharPredicate.rep ~ "?".?).!).map {
      case "nil" => SexpNil
      case sym => SexpSymbol(sym)
    }

  private val SexpEmptyList: Parser[SexpNil.type] =
    P(LeftBrace ~ RightBrace)
      .map(_ => SexpNil)

  private val NormalChar: Parser[Char] =
    P(NormalCharPredicate.!)
      .map(_(0))

  private val Whitespace: P0 =
    P((Comment | WhiteSpacePredicate).rep)

  private val Comment: P0 =
    P(";" ~ NotNewLinePredicate.rep ~ ("\n" | End))

  private val LeftBrace: P0 =
    P(Whitespace ~ "(" ~ Whitespace)

  private val RightBrace: P0 =
    P(Whitespace ~ ")" ~ Whitespace)

}
