package org.ensime.config

import org.ensime.util.file._
import org.ensime.util.{ EnsimeSpec, EscapingStringInterpolation }

import org.ensime.api._
import org.ensime.config.richconfig._

class EnsimeConfigSpec extends EnsimeSpec {

  import EscapingStringInterpolation._

  def test(contents: String, testFn: (EnsimeConfig) => Unit): Unit = {
    println(s"Test content: $contents")
    testFn(EnsimeConfigProtocol.parse(contents))
  }

  "EnsimeConfig" should "parse a simple config" in withTempDir { dir =>
    val abc = dir / "abc"
    val cache = dir / ".ensime_cache"
    val javaHome = dir / "java-1.8.0-openjdk"

    abc.mkdirs
    cache.mkdirs
    javaHome.mkdirs

    test(s"""
(:name "project"
 :scala-version "2.10.4"
 :java-home "$javaHome"
 :cache-dir "$cache"
 :reference-source-roots ()
 :subprojects ((:name "module1"
                :scala-version "2.10.4"
                :depends-on-modules ()
                :targets ("$abc")
                :test-targets ("$abc")
                :source-roots ()
                :reference-source-roots ()
                :compiler-args ()
                :runtime-deps ()
                :test-deps ()))
 :projects ((:id (:project "module1" :config "compile")
             :depends ()
             :sources ()
             :targets ("$abc")
             :scalac-options ()
             :javac-options ()
             :library-jars ()
             :library-sources ()
             :library-docs ())))""", { implicit config =>
      config.name shouldBe "project"
      config.scalaVersion shouldBe "2.10.4"
      val module1 = config.lookup(EnsimeProjectId("module1", "compile"))
      module1.id.project shouldBe "module1"
      module1.dependencies shouldBe empty
      config.projects.size shouldBe 1
    })
  }

  it should "parse a minimal config for a binary only project" in withTempDir { dir =>
    val abc = dir / "abc"
    val cache = dir / ".ensime_cache"
    val javaHome = dir / "java-1.8.0-openjdk"

    abc.mkdirs
    cache.mkdirs
    javaHome.mkdirs

    test(s"""
(:name "project"
 :scala-version "2.10.4"
 :java-home "$javaHome"
 :cache-dir "$cache"
 :projects ((:id (:project "module1" :config "compile")
             :depends ()
             :targets ("$abc"))))""", { implicit config =>

      config.name shouldBe "project"
      config.scalaVersion shouldBe "2.10.4"
      val module1 = config.lookup(EnsimeProjectId("module1", "compile"))
      module1.id.project shouldBe "module1"
      module1.dependencies shouldBe empty
      module1.targets should have size 1
    })
  }
}
