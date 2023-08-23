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

package uk.gov.hmrc.repositories

import org.scalatest._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.CacheRepository

class CacheRepositorySpec extends AsyncFreeSpec with Matchers with TransformIntegrationTest
  with BeforeAndAfterEach {

  private val repository = createApplication.injector.instanceOf[CacheRepository]

  private val data = Json.obj("testField" -> "testValue")
  private val internalId = "Int-328969d0-96ba-4559-557e-074d0597107e"
  private val utr = "UTRUTRUTR"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.resetCache(utr, internalId))
  }

  "a cache repository" - {

    "must be able to store and retrieve a payload" in {

      val storedOk = repository.set(utr, internalId, data)
      await(storedOk) mustBe true

      val retrieved = repository.get(utr, internalId)

      await(retrieved) mustBe Some(data)
    }
  }
}
