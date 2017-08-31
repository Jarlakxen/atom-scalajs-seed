package org.ensime.util

import java.util.{ Timer, TimerTask }
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.time._
import slogging._

/**
 * Boilerplate remover and preferred testing style in ENSIME.
 */
trait EnsimeSpec extends FlatSpec
    with Matchers
    with OptionValues
    with Inside
    with Retries
    with TryValues
    with Inspectors
    with TypeCheckedTripleEquals
    with BeforeAndAfterAll 
    with LazyLogging { self =>

}