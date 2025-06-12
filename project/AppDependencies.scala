import sbt.*

object AppDependencies {

  val boostrapVersion = "9.5.0"
  val mongoVersion = "2.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % boostrapVersion,
    "com.github.java-json-tools"    %  "json-schema-validator"      % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "5.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.19.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"   % boostrapVersion,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"  % mongoVersion,
    "org.scalatest"               %% "scalatest"                % "3.2.19",
    "org.jsoup"                   %  "jsoup"                    % "1.20.1",
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
