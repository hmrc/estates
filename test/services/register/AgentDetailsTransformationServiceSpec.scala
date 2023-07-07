/*
 * Copyright 2023 HM Revenue & Customs
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

package services.register

import models.{AddressType, AgentDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import services.TransformationService
import transformers.ComposedDeltaTransform
import transformers.register.AgentDetailsTransform

import scala.concurrent.Future

class AgentDetailsTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with OptionValues {

  private val agentDetails = AgentDetails(
    arn = "SARN1234567",
    agentName = "Agent name",
    agentAddress = AddressType(
      line1 = "56",
      line2 = "Maple Street",
      line3 = Some("Northumberland"),
      line4 = None,
      postCode = Some("ne64 8hr"),
      country = "GB"
    ),
    agentTelephoneNumber =  "07912180120",
    clientReference = "clientReference"
  )

  "AgentDetailsTransformationService" - {

    "must write a corresponding transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new AgentDetailsTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

      val result = service.addTransform("internalId", agentDetails)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          "internalId", AgentDetailsTransform(agentDetails)
        )

      }
    }

    "must not return a transform if one does not exist" - {

      "due to there being no data" in {
        val transformationService = mock[TransformationService]
        val service = new AgentDetailsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))

        whenReady(service.get("internalId")) { result =>

          result mustBe None

        }
      }

      "due to there being no transforms" in {
        val transformationService = mock[TransformationService]
        val service = new AgentDetailsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(None))

        whenReady(service.get("internalId")) { result =>
          result mustBe None
        }
      }
    }

    "must return agent details if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new AgentDetailsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(AgentDetailsTransform(agentDetails))))))

        whenReady(service.get("internalId")) { result =>

          result.value mustBe agentDetails

        }
      }

    }

  }
}
