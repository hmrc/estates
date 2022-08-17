/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import base.BaseSpec
import org.mockito.Mockito.verify
import org.mockito.Matchers.{any, eq => equalTo}
import play.api.libs.json.Json
import models.RegistrationFailureResponse
import models.auditing.EstatesAuditData
import models.variation.VariationSuccessResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditServiceSpec extends BaseSpec {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "auditRegistrationFailed" should {
    "send error details" when {
      "a failure response comes in" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector, appConfig)

        val request = Json.obj("someField" -> "someValue")

        val response = RegistrationFailureResponse(403, "des response", "General error message")
        service.auditRegistrationFailed("internalId", request, response)

        val expectedAuditData = EstatesAuditData(
          request,
          "internalId",
          Some(Json.obj("errorReason" -> Json.toJson(response)))
        )

        verify(connector).sendExplicitAudit[EstatesAuditData](
          equalTo("RegistrationSubmissionFailed"),
          equalTo(expectedAuditData))(any(), any(), any())

      }
    }
  }

  "auditVariationSubmitted" should {
    "send Variation Submitted by Organisation" when {
      "there are no special JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector, appConfig)

        val request = Json.obj()

        val response = VariationSuccessResponse("TRN123456")
        service.auditVariationSubmitted("internalId", request, response)

        val expectedAuditData = EstatesAuditData(
          request,
          "internalId",
          Some(Json.toJson(response))
        )

        verify(connector).sendExplicitAudit[EstatesAuditData](
          equalTo("VariationSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Variation Submitted by Agent" when {
      "there is an AgentDetails JSON field" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector, appConfig)

        val request = Json.obj(
          "agentDetails" -> Json.obj() // Doesn't care about contents of object
        )

        val response = VariationSuccessResponse("TRN123456")
        service.auditVariationSubmitted("internalId", request, response)

        val expectedAuditData = EstatesAuditData(
          request,
          "internalId",
          Some(Json.toJson(response))
        )

        verify(connector).sendExplicitAudit[EstatesAuditData](
          equalTo("VariationSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Organisation" when {
      "there is an endTrustDate field" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector, appConfig)

        val request = Json.obj(
          "trustEndDate" -> "2012-02-12"
        )

        val response = VariationSuccessResponse("TRN123456")
        service.auditVariationSubmitted("internalId", request, response)

        val expectedAuditData = EstatesAuditData(
          request,
          "internalId",
          Some(Json.toJson(response))
        )

        verify(connector).sendExplicitAudit[EstatesAuditData](
          equalTo("ClosureSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Agent" when {
      "there are agentDetails and endTrustDate JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector, appConfig)

        val request = Json.obj(
          "trustEndDate" -> "2012-02-12",
          "agentDetails" -> Json.obj() // Doesn't care about contents of object
        )

        val response = VariationSuccessResponse("TRN123456")
        service.auditVariationSubmitted("internalId", request, response)

        val expectedAuditData = EstatesAuditData(
          request,
          "internalId",
          Some(Json.toJson(response))
        )

        verify(connector).sendExplicitAudit[EstatesAuditData](
          equalTo("ClosureSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }
  }

  "auditEnrolmentSucceeded" should {
    "send EnrolmentSucceeded audit" in {
      val connector = mock[AuditConnector]
      val service = new AuditService(connector, appConfig)

      service.auditEnrolSuccess("ChickenSub", "TheTRN", "internalId")

      val expectedAuditData = EstatesAuditData(
        Json.obj(
          "trn" -> "TheTRN",
          "subscriptionID" -> "ChickenSub"
        ),
        "internalId",
        Some(Json.obj())
      )

      verify(connector).sendExplicitAudit[EstatesAuditData](
        equalTo("EnrolmentSucceeded"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }

  "auditEnrolmentFailure" should {
    "send EnrolmentFailed audit" in {
      val connector = mock[AuditConnector]
      val service = new AuditService(connector, appConfig)

      service.auditEnrolFailed("ChickenSub", "TheTRN", "internalId", "bad juju")

      val expectedAuditData = EstatesAuditData(
        Json.obj(
          "trn" -> "TheTRN",
          "subscriptionID" -> "ChickenSub"
        ),
        "internalId",
        Some(Json.obj( "errorReason" -> "bad juju"))
      )

      verify(connector).sendExplicitAudit[EstatesAuditData](
        equalTo("EnrolmentFailed"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }
}
