import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % "0.73.0",
    "uk.gov.hmrc"                   %% "logback-json-logger"        % "5.2.0",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % "7.3.0",
    "com.github.java-json-tools"    %  "json-schema-validator"      % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "3.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.13.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"   % "7.3.0",
    "org.scalatest"               %% "scalatest"                % "3.2.12",
    "wolfendale"                  %% "scalacheck-gen-regexp"    % "0.1.2",
    "org.jsoup"                   %  "jsoup"                    % "1.15.3",
    "com.typesafe.play"           %% "play-test"                % PlayVersion.current,
    "org.mockito"                 %% "mockito-scala-scalatest"  % "1.17.12",
    "com.vladsch.flexmark"        %  "flexmark-all"             % "0.64.0",
    "uk.gov.hmrc"                 %% "service-integration-test" % "1.3.0-play-28",
    "com.github.tomakehurst"      %  "wiremock-jre8"            % "2.33.2",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"  % "0.73.0"
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
