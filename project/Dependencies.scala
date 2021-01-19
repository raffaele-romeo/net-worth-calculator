import sbt._

object Dependencies {

  object Versions {
    val cats          = "2.3.1"
    val catsEffect    = "2.3.1"
    val catsRetry     = "2.1.0"
    val circe         = "0.13.0"
    val ciris         = "1.2.1"
    val doobie        = "0.10.0"
    val elasticsearch = "7.10.2"
    val http4s        = "0.21.15"
    val newtype       = "0.4.3"
    val refined       = "0.9.20"
    val squants       = "1.7.0"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.3"

    val scalaCheck    = "1.15.2"
    val scalaTest     = "3.2.2"
    val scalaTestPlus = "3.2.2.0"
  }

  object Libraries {
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact % Versions.ciris
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe

    val cats       = "org.typelevel"    %% "cats-core"   % Versions.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % Versions.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % Versions.catsRetry

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val doobieCore = "org.tpolecat" %% "doobie-core" % Versions.doobie

    val http4sDsl    = http4s("http4s-dsl")
    val http4sServer = http4s("http4s-blaze-server")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val newtype       = "io.estatico"       %% "newtype"      % Versions.newtype
    val elasticsearch = "org.elasticsearch" % "elasticsearch" % Versions.elasticsearch
    val refinedCore   = "eu.timepit"        %% "refined"      % Versions.refined
    val refinedCats   = "eu.timepit"        %% "refined-cats" % Versions.refined
    val squants       = "org.typelevel"     %% "squants"      % Versions.squants

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"    %% "better-monadic-for" % Versions.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"      % Versions.kindProjector

    // Test
    val scalaCheck    = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    val scalaTest     = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % Versions.scalaTestPlus
  }
}
