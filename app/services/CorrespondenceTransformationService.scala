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

package services

import play.api.libs.json.JsString
import transformers.ComposedDeltaTransform
import transformers.register.CorrespondenceNameTransform

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class CorrespondenceTransformationService @Inject()(
                                                  transformationService: TransformationService
                                                )(implicit val ec: ExecutionContext) {

  def addAmendCorrespondenceNameTransformer(internalId: String, newCorrespondenceName: JsString): Future[Success.type] =
    transformationService.addNewTransform(internalId, CorrespondenceNameTransform(newCorrespondenceName)).map(_ => Success)

  def getCorrespondenceName(internalId: String): Future[Option[JsString]] = {
    transformationService.getTransformations(internalId) map {
      case Some(ComposedDeltaTransform(transforms)) =>
        transforms.flatMap{
          case CorrespondenceNameTransform(name) => Some(name)
          case _ => None
        }.lastOption
      case _ => None
    }
  }
}
