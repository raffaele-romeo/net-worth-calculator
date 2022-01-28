resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.typesafe.sbt"         %% "sbt-native-packager" % "1.8.1")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"        % "2.4.2")
addSbtPlugin("io.spray"                  % "sbt-revolver"        % "0.9.1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"        % "0.1.16")
addSbtPlugin("com.eed3si9n"              % "sbt-assembly"        % "1.1.0")
addSbtPlugin("ch.epfl.scala"             % "sbt-missinglink"     % "0.3.3")
addSbtPlugin("ch.epfl.scala"             % "sbt-scalafix"        % "0.9.34")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("org.wartremover"  % "sbt-wartremover"      % "2.4.16")

libraryDependencies += "com.spotify" % "missinglink-core" % "0.2.5"
