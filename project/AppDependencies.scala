import sbt.*

object AppDependencies {

  val boostrapVersion = "8.5.0"
  val mongoVersion = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % boostrapVersion,
    "com.github.java-json-tools"    %  "json-schema-validator"      % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "3.3.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.17.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"   % boostrapVersion,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"  % mongoVersion,
    "org.scalatest"               %% "scalatest"                % "3.2.18",
    "org.jsoup"                   %  "jsoup"                    % "1.17.2",
    "org.mockito"                 %% "mockito-scala-scalatest"  % "1.17.31",
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
