// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp

import org.scalatest._
import org.scalactic.TypeCheckedTripleEquals
import slogging._

/**
 * Boilerplate remover and preferred testing style in S-Express.
 */
abstract class SexpSpec extends FlatSpec
    with Matchers
    with Inside
    with TryValues
    with Inspectors
    with TypeCheckedTripleEquals
    with LazyLogging {

}
