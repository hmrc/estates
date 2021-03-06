import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "org.reactivemongo"          %% "play2-reactivemongo"       % "0.18.8-play27",
    "uk.gov.hmrc"                %% "logback-json-logger"       % "4.8.0",
    "uk.gov.hmrc"                %% "bootstrap-backend-play-27" % "2.25.0",
    "com.github.java-json-tools" % "json-schema-validator"      % "2.2.14",
    "uk.gov.hmrc"                %% "tax-year"                  % "1.1.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"               %% "scalatest"          % "3.0.8",
    "org.scalatestplus.play"      %% "scalatestplus-play" % "4.0.3",
    "org.pegdown"                 %  "pegdown"            % "1.6.0",
    "org.jsoup"                   %  "jsoup"              % "1.12.1",
    "com.typesafe.play"           %% "play-test"          % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"        % "1.10.19",
    "org.scalacheck"              %% "scalacheck"         % "1.14.3",
    "wolfendale"                  %% "scalacheck-gen-regexp" % "0.1.2",
    "com.github.tomakehurst"      % "wiremock-standalone"      % "2.25.1"
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
