import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"               %% "simple-reactivemongo"       % "8.1.0-play-28",
    "uk.gov.hmrc"                %% "logback-json-logger"       % "5.1.0",
    "uk.gov.hmrc"                %% "bootstrap-backend-play-28" % "5.24.0",
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "uk.gov.hmrc"                %% "tax-year"                  % "3.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.12.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28" % "5.24.0",
    "org.scalatest"               %% "scalatest"              % "3.2.12",
    "org.scalatestplus.play"      %% "scalatestplus-play"       % "5.1.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "wolfendale"                  %% "scalacheck-gen-regexp"    % "0.1.2",
    "org.jsoup"                   %  "jsoup"                  % "1.15.1",
    "com.typesafe.play"           %% "play-test"              % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"              % "1.10.19",
    "org.mockito"                 %  "mockito-core"             % "4.6.1",
    "org.scalatestplus"           %% "mockito-3-12"             % "3.2.10.0",
    "org.scalatestplus"           %% "scalacheck-1-15"        % "3.2.9.0",
    "org.scalatestplus"           %% "scalatestplus-mockito"  % "1.0.0-M2",
    "com.github.tomakehurst"      %  "wiremock-standalone"    % "2.27.2",
    "com.vladsch.flexmark"        % "flexmark-all"            % "0.62.2"
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
