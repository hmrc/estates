import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % "0.74.0",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % "7.20.0",
    "com.github.java-json-tools"    %  "json-schema-validator"      % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "3.2.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.15.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"   % "7.20.0",
    "org.scalatest"               %% "scalatest"                % "3.2.16",
    "wolfendale"                  %% "scalacheck-gen-regexp"    % "0.1.2",
    "org.jsoup"                   %  "jsoup"                    % "1.16.1",
    "org.mockito"                 %% "mockito-scala-scalatest"  % "1.17.14",
    "com.vladsch.flexmark"        %  "flexmark-all"             % "0.64.8",
    "com.github.tomakehurst"      %  "wiremock-jre8"            % "2.35.0",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"  % "0.74.0"
  ).map(_ % "test, it")

}
