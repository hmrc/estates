import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

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
    ".*mapping.Constants.*",
    ".*pages.*",
    ".*viewmodels.*",
    ".*Message.*",
    ".*config.*",
    ".*models.RegistrationResponse.*",
    ".*LocalDateService.*",
    ".*GetEstateErrorResponse.*",
    ".*JsonOperations.*",
    ".*EstateVariationModels.*",
    ".*AuditService.*"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "2.12.15",
    SilencerSettings(),
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides              ++= AppDependencies.overrides,
    PlayKeys.playDefaultPort := 8832,
      ScoverageKeys.coverageExcludedFiles := excludedPackages.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
  )
  .settings(inConfig(Test)(testSettings))
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(resolvers += Resolver.jcenterRepo)

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
