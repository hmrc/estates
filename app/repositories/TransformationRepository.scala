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

package repositories

import config.AppConfig
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.combine
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json._
import transformers.ComposedDeltaTransform
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

trait TransformationRepository {

  def get(internalId: String): Future[Option[ComposedDeltaTransform]]

  def set(internalId: String, transforms: ComposedDeltaTransform): Future[Boolean]

  def resetCache(internalId: String): Future[Option[JsObject]]
}

@Singleton
class TransformationRepositoryImpl @Inject()(mongo: MongoComponent, config: AppConfig)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mongo,
    collectionName = "transforms",
    domainFormat = implicitly[Format[JsObject]],
    indexes = Seq(
      IndexModel(
        Indexes.ascending("updatedAt"),
        IndexOptions()
          .name("transformation-data-updated-at-index")
          .expireAfter(config.ttlInSeconds, SECONDS)
      ),
      IndexModel(
        Indexes.ascending("id"),
        IndexOptions()
          .name("id-index")
      )
    )
  ) with TransformationRepository with Logging {

  override def get(internalId: String): Future[Option[ComposedDeltaTransform]] = {

    val selector = equal("id", internalId)

    collection.find(selector).headOption().map { opt =>
      for {
        document <- opt
        transforms <- (document \ "transforms").asOpt[ComposedDeltaTransform]
      } yield transforms
    }
  }

  override def set(internalId: String, transforms: ComposedDeltaTransform): Future[Boolean] = {

    val selector = equal("id", internalId)

    val modifier = combine(
      Updates.set("id", internalId),
      Updates.set("updatedAt", Json.obj("$date" -> Timestamp.valueOf(LocalDateTime.now()))),
      Updates.set("transforms", Json.toJson(transforms))
    )

    val updateOptions = new UpdateOptions().upsert(true)

    collection.updateOne(selector, modifier, updateOptions).toFutureOption().map {
      case Some(_) => true
      case None => false
    }
  }

  override def resetCache(internalId: String): Future[Option[JsObject]] = {
    val selector = equal("id", internalId)

    collection.findOneAndDelete(selector).toFutureOption()
  }
}
