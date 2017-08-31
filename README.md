# Atom with Scala.js

This is just an example of how to write a [Scala.js](https://www.scala-js.org/) plugin for [Atom](https://atom.io/).


Install in Atom
----

After checkout the code, run `npm install & sbt fastOptJS` and copy the project folder inside $HOME/.atom/packages. 


Run test
----
`sbt test`

Debug test
----
Uncomment this line, in the build.sbt:
```scala
// jsEnv in Test := new org.scalajs.jsenv.nodejs.NodeJSEnv(org.scalajs.jsenv.nodejs.NodeJSEnv.Config().withArgs(List("--inspect-brk")))
```
and run `sbt test`.