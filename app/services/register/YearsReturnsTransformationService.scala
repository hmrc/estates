/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.Inject
import models.{Success, YearsReturns}
import services.TransformationService
import transformers.ComposedDeltaTransform
import transformers.register.YearsReturnsTransform

import scala.concurrent.{ExecutionContext, Future}

class YearsReturnsTransformationService @Inject()(transformationService: TransformationService)(implicit ec: ExecutionContext) {

  def get(internalId: String): Future[Option[YearsReturns]] = {
    transformationService.getTransformations(internalId) map {
      case Some(ComposedDeltaTransform(transforms)) =>
        transforms.flatMap{
          case YearsReturnsTransform(yearsReturns) => Some(yearsReturns)
          case _ => None
        }.lastOption
      case _ => None
    }
  }

  def addTransform(internalId: String, yearsReturns: YearsReturns) : Future[Success.type] = {
    transformationService.addNewTransform(internalId, YearsReturnsTransform(yearsReturns)) map {
      _ => Success
    }
  }

  def removeTransforms(internalId: String) : Future[Success.type] = {
    transformationService.removeYearsReturnsTransform(internalId) map {
      _ => Success
    }
  }

}
