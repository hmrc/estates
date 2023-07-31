import scoverage.ScoverageKeys

val appName = "estates"

lazy val IntegrationTest = config("it") extend(Test)

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
    "com.kenshoo.play.metrics*.*",
    ".*repositories.*",
    ".*LanguageSwitchController",
    ".*GuiceInjector",
    ".*models.Mode",
    ".*filters.*",
    ".*handlers.*",
    ".*components.*",
    ".*FrontendAuditConnector.*",
    ".*javascript.*",
    ".*ControllerConfiguration",
    ".*mapping.Constants.*"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
      scalaVersion := "2.13.11",
      // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      majorVersion := 0,
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
      PlayKeys.playDefaultPort := 8832,
      ScoverageKeys.coverageExcludedFiles := excludedPackages.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      scalacOptions ++= Seq(
          "-Wconf:src=routes/.*:s",
          "-Wconf:cat=unused-imports&src=views/.*:s"
      )
  )
  .settings(inConfig(Test)(testSettings))
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings))

addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")

lazy val itSettings = Defaults.itSettings ++ Seq(
    unmanagedSourceDirectories   := Seq(
        baseDirectory.value / "it"
    ),
    parallelExecution            := false,
    fork                         := true,
    javaOptions ++= Seq(
        "-Dconfig.resource=test.application.conf",
        "-Dlogger.resource=logback-test.xml"
    )
)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
    fork        := true,
    javaOptions ++= Seq(
        "-Dconfig.resource=test.application.conf",
        "-Dlogger.resource=logback-test.xml"
    )
)
