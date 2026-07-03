/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  val ttlInSeconds: Int = config.getOptional[Int]("mongodb.ttlSeconds").getOrElse(4 * 60 * 60)

  val subscriptionBaseUrl: String = servicesConfig.baseUrl("subscription")

  val useHipEstates: Boolean        = servicesConfig.getBoolean("features.hip.estates")
  private val hipClientIdV1: String = config.get[String]("microservice.services.hip.registration.clientId")
  private val hipSecretV1: String   = config.get[String]("microservice.services.hip.registration.secret")
  def hipAuthorizationToken: String = Base64.getEncoder.encodeToString(s"$hipClientIdV1:$hipSecretV1".getBytes("UTF-8"))

  val hipRegistrationBaseUrl: String = servicesConfig.baseUrl("hip.registration")
  val desRegistrationBaseUrl: String = servicesConfig.baseUrl("des.registration")

  val getEstateBaseUrl: String    = servicesConfig.baseUrl("playback")
  val desVaryEstateBaseUrl: String   = servicesConfig.baseUrl("des.variation")
  val hipVaryEstateBaseUrl: String   = servicesConfig.baseUrl("hip.variation")
  val estatesStoreBaseUrl: String = servicesConfig.baseUrl("estates-store")

  val registrationEnvironment: String = loadConfig("microservice.services.des.registration.environment")
  val registrationToken: String       = loadConfig("microservice.services.des.registration.token")

  val playbackEnvironment: String = loadConfig("microservice.services.playback.environment")
  val playbackToken: String       = loadConfig("microservice.services.playback.token")

  val variationEnvironment: String = loadConfig("microservice.services.des.variation.environment")
  val variationToken: String       = loadConfig("microservice.services.des.variation.token")

  val subscriptionEnvironment: String = loadConfig("microservice.services.subscription.environment")
  val subscriptionToken: String       = loadConfig("microservice.services.subscription.token")

  val taxEnrolmentsBaseUrl: String             = servicesConfig.baseUrl("tax-enrolments")
  val taxEnrolmentsPayloadBodyCallback: String = loadConfig("microservice.services.tax-enrolments.callback")

  val delayToConnectTaxEnrolment: Int = loadConfig("microservice.services.estates.delayToConnectTaxEnrolment").toInt

  val variationsApiSchema: String = "/resources/schemas/4MLD/variations-api-schema-4.0.json"

  val maxRetry: Int = loadConfig("microservice.services.estates.maxRetry").toInt

  val auditingEnabled: Boolean = loadConfig("microservice.services.estates.features.auditing.enabled").toBoolean

  private def loadConfig(key: String) = config
    .getOptional[String](key)
    .getOrElse(
      throw new Exception(s"Missing configuration key : $key")
    )

}
