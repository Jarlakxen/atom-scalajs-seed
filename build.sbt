// ··· Plugins ···

enablePlugins(ScalaJSPlugin)

// ··· Project Info ···

name := "atom-scalajs"

scalaVersion := "2.12.3"

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

scalacOptions in Test ++= Seq("-Yrangepos")

// ··· Project Dependancies ···

val sloggingV = "0.5.3"

libraryDependencies ++= Seq(
  "biz.enef" %%% "slogging" % sloggingV
)

// ··· ScalaJS Settings ···

scalaJSModuleKind := ModuleKind.CommonJSModule
