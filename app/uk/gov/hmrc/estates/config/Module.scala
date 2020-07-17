/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.estates.config

import com.google.inject.AbstractModule
import uk.gov.hmrc.estates.controllers.actions.{AuthenticatedIdentifierAction, IdentifierAction}
import uk.gov.hmrc.estates.repositories._

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[MongoDriver]).to(classOf[EstatesMongoDriver]).asEagerSingleton()

    bind(classOf[TransformationRepository]).to(classOf[TransformationRepositoryImpl]).asEagerSingleton()
    bind(classOf[CacheRepository]).to(classOf[CacheRepositoryImpl]).asEagerSingleton()
    bind(classOf[VariationsTransformationRepository]).to(classOf[VariationsTransformationRepositoryImpl]).asEagerSingleton()
  }
}
