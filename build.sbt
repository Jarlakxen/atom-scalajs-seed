// ··· Plugins ···

enablePlugins(ScalaJSPlugin)

// ··· Project Info ···

name := "atom-scalajs"

scalaVersion := "2.12.2"

licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

// ··· Project Options ···

scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-unchecked",
    "-deprecation"
)

parallelExecution in Test := false

// ··· Project Dependancies ···

val nodeJsV       = "0.4.0"
val fastparseV    = "0.4.4"
val shapelessV    = "2.3.2"
val sloggingV     = "0.5.3"
val scalatestV    = "3.0.4"
val scalacheckV   = "1.13.5"

libraryDependencies ++= Seq(
  "io.scalajs"      %%% "nodejs"      % nodeJsV,
  "com.lihaoyi"     %%% "fastparse"   % fastparseV,
  "com.chuusai"     %%% "shapeless"   % shapelessV,
  "biz.enef"        %%% "slogging"    % sloggingV,
  // Testing
  "org.scalatest"   %%% "scalatest"   % scalatestV     % "test",
  "org.scalacheck"  %%% "scalacheck"  % scalacheckV    % "test"
)

// ··· ScalaJS Settings ···

scalaJSUseMainModuleInitializer := true
scalaJSModuleKind := ModuleKind.CommonJSModule

// Enable Debug in Tests
// jsEnv in Test := new org.scalajs.jsenv.nodejs.NodeJSEnv(org.scalajs.jsenv.nodejs.NodeJSEnv.Config().withArgs(List("--inspect-brk")))

