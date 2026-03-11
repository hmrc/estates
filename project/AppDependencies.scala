import sbt.*

object AppDependencies {

  private val boostrapVersion = "10.7.0"
  private val mongoVersion    = "2.12.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"        % mongoVersion,
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % boostrapVersion,
    "com.github.java-json-tools"    % "json-schema-validator"     % "2.2.14",
    "uk.gov.hmrc"                  %% "tax-year"                  % "6.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.21.1"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % boostrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
