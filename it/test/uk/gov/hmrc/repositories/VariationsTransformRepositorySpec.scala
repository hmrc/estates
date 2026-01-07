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

package uk.gov.hmrc.repositories

import models.variation.EstatePerRepIndType
import models.{IdentificationType, NameType}
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.VariationsTransformationRepository
import transformers.ComposedDeltaTransform
import transformers.variations.AddAmendIndividualPersonalRepTransform

import java.time.LocalDate

class VariationsTransformRepositorySpec extends AnyWordSpec with Matchers with TransformIntegrationTest
  with BeforeAndAfterEach {

  private val repository = appBuilder.build().injector.instanceOf[VariationsTransformationRepository]

  private val internalId = "Int-074d0597107e-557e-4559-96ba-328969d0"
  private val utr = "UTRUTRUTR"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.resetCache(utr, internalId))
  }

  private val data = ComposedDeltaTransform(
    Seq(
      AddAmendIndividualPersonalRepTransform(
        EstatePerRepIndType(
          Some(""),
          None,
          NameType("New", Some("lead"), "Trustee"),
          LocalDate.parse("2000-01-01"),
          IdentificationType(None, None, None),
          "",
          None,
          LocalDate.parse("2010-10-10"),
          None
        )
      )
    )
  )

  "a variations repository" should {

    "must be able to store and retrieve a payload" in {

      val storedOk = repository.set(utr, internalId, data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get(utr, internalId)

      retrieved.futureValue mustBe Some(data)
    }
  }
}
