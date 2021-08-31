import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"               %% "simple-reactivemongo"       % "8.0.0-play-28",
    "uk.gov.hmrc"                %% "logback-json-logger"       % "5.1.0",
    "uk.gov.hmrc"                %% "bootstrap-backend-play-28" % "5.10.0",
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "uk.gov.hmrc"                %% "tax-year"                  % "1.4.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.12.4"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"               %% "scalatest"              % "3.0.8",
    "org.scalatestplus.play"      %% "scalatestplus-play"     % "4.0.3",
    "org.pegdown"                 %  "pegdown"                % "1.6.0",
    "org.jsoup"                   %  "jsoup"                  % "1.12.1",
    "com.typesafe.play"           %% "play-test"              % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"            % "1.10.19",
    "org.scalacheck"              %% "scalacheck"             % "1.14.3",
    "wolfendale"                  %% "scalacheck-gen-regexp"  % "0.1.2",
    "com.github.tomakehurst"      %  "wiremock-standalone"    % "2.27.2"
  ).map(_ % "test, it")

  val akkaVersion = "2.6.7"
  val akkaHttpVersion = "10.1.12"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core_2.12" % akkaHttpVersion
  )
}
