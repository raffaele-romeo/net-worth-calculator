addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.16")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.4.2")
// sbt-dotty is not required since sbt 1.5.0-M1
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.3")
