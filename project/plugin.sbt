addSbtPlugin("com.github.sbt"  %% "sbt-native-packager" % "1.10.4")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"        % "2.5.4")
addSbtPlugin("io.spray"        % "sbt-revolver"        % "0.10.0")
addSbtPlugin("org.typelevel"   % "sbt-tpolecat"        % "0.5.2")
addSbtPlugin("com.eed3si9n"    % "sbt-assembly"        % "2.3.0")
addSbtPlugin("ch.epfl.scala"   % "sbt-missinglink"     % "0.3.6")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"        % "0.13.0")
libraryDependencies += "com.spotify" % "missinglink-core" % "0.2.11"
