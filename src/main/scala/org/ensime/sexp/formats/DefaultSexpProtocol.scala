// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/lgpl-3.0.en.html
package org.ensime.sexp.formats

trait DefaultSexpProtocol
  extends BasicFormats
  with StandardFormats
  with CollectionFormats
  with LegacyProductFormats

object DefaultSexpProtocol extends DefaultSexpProtocol
