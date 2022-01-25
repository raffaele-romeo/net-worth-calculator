import sbt._

object Dependencies {

  object Versions {
    val cats         = "2.7.0"
    val catsEffect   = "3.3.0"
    val catsRetry    = "3.1.0"
    val circe        = "0.14.1"
    val ciris        = "2.3.1"
    val commonsCodec = "1.15"
    val doobie       = "1.0.0-RC1"
    val http4s       = "0.23.7"
    val log4cats     = "2.1.1"
    val logback      = "1.2.8"
    val nimbus       = "9.15.2"
    val redis4cats   = "1.0.0"
    val squants      = "1.8.3"

    val scalaTest = "3.2.9"
  }

  object Libraries {
    def http4s(artifact: String): ModuleID   = "org.http4s"    %% artifact % Versions.http4s
    def ciris(artifact: String): ModuleID    = "is.cir"        %% artifact % Versions.ciris
    def circe(artifact: String): ModuleID    = "io.circe"      %% artifact % Versions.circe
    def doobie(artifact: String): ModuleID   = "org.tpolecat"  %% artifact % Versions.doobie
    def log4Cats(artifact: String): ModuleID = "org.typelevel" %% artifact % Versions.log4cats

    val cats       = "org.typelevel"    %% "cats-core"   % Versions.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % Versions.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % Versions.catsRetry

    val circeCore         = circe("circe-core")
    val circeGeneric      = circe("circe-generic")
    val circeGenericExtra = circe("circe-generic-extras")
    val circeParser       = circe("circe-parser")

    val cirisCore = ciris("ciris")

    val commonsCodec = "commons-codec" % "commons-codec" % Versions.commonsCodec

    val doobieCore     = doobie("doobie-core")
    val doobieHikari   = doobie("doobie-hikari")
    val doobiePostgres = doobie("doobie-postgres")

    val log4catsSlf4j = log4Cats("log4cats-slf4j")
    val log4catsCore  = log4Cats("log4cats-core")

    val nimbus = "com.nimbusds" % "nimbus-jose-jwt" % Versions.nimbus

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4cats

    val http4sDsl    = http4s("http4s-dsl")
    val http4sServer = http4s("http4s-blaze-server")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val squants = "org.typelevel" %% "squants" % Versions.squants

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest
  }
}
