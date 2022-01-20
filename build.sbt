import Dependencies._

lazy val root = (project in file("."))
  .settings(
    Compile / run / mainClass := Some("networthcalculator.Main")
  )
  .dependsOn(core)
  .aggregate(core)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "net-worth-calculator-test-suite",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      Libraries.scalaTest
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(
    DockerPlugin,
    AshScriptPlugin
  )
  .settings(commonSettings: _*)
  .settings(
    name := "net-worth-calculator"
  )
  .settings(
    Docker / packageName := "net-worth-calculator",
    dockerBaseImage := "openjdk:11-jdk-slim",
    makeBatScripts     := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsRetry,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.cirisCore,
      Libraries.commonsCodec,
      Libraries.doobieCore,
      Libraries.doobieHikari,
      Libraries.doobiePostgres,
      Libraries.logback,
      Libraries.log4catsSlf4j,
      Libraries.log4catsCore,
      Libraries.nimbus,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
      Libraries.redis4catsEffects,
      Libraries.redis4catsLog4cats,
      Libraries.squants
    )
  )

val commonSettings = Def.settings(
  inThisBuild(
    List(
      scalaVersion := "3.1.0",
      version      := "0.1.0-SNAPSHOT"
    )
  ),
  ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.5.5",
  resolvers += Resolver.sonatypeRepo("snapshots"),
  scalafmtOnCompile := false,
  addCommandAlias(
    "validate",
    List(
      "clean",
      "scalafmtCheckAll",
      "compile",
      "missinglinkCheck"
    ).mkString(";", "; ", "")
  ),
  addCommandAlias("run", "modules/core/run")
)
