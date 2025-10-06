import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 0

lazy val microservice =
  Project("estates", file("."))
    .enablePlugins(PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(
      libraryDependencies ++= AppDependencies(),
      PlayKeys.playDefaultPort := 8832,
      CodeCoverageSettings(),
      scalacOptions ++= Seq(
        "-Wconf:src=routes/.*:s",
        "-feature"
      )
    )
    .settings(inConfig(Test)(testSettings))

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)

val excludedPackages = Seq(
  "<empty>",
  ".*Reverse.*",
  ".*Routes.*",
  ".*standardError*.*",
  ".*main_template*.*",
  "uk.gov.hmrc.BuildInfo",
  "app.*",
  "prod.*",
  ".*testOnlyDoNotUseInAppConf.*",
  "views.html.*",
  "testOnly.*",
  ".*GuiceInjector",
  ".*models.Mode",
  ".*FrontendAuditConnector.*",
  ".*javascript.*",
  ".*mapping.Constants.*"
)

lazy val it =
  project
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(DefaultBuildSettings.itSettings())
