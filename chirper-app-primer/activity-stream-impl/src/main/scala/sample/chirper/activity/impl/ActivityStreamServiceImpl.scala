/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.impl

import java.time.Duration
import java.time.Instant

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import akka.NotUsed
import akka.stream.scaladsl.Source
import sample.chirper.activity.api.{ActivityStreamService, HistoricalActivityStreamReq}
import sample.chirper.chirp.api.Chirp
import sample.chirper.chirp.api.ChirpService
import sample.chirper.chirp.api.HistoricalChirpsRequest
import sample.chirper.chirp.api.LiveChirpsRequest
import sample.chirper.friend.api.FriendService

class ActivityStreamServiceImpl (
    friendService: FriendService,
    chirpService: ChirpService)(implicit ec: ExecutionContext) extends ActivityStreamService {


  override def getLiveActivityStream(userId: String): ServiceCall[NotUsed, Source[Chirp, NotUsed]] = {
    req =>
      for {
        userIds <- userAndFriendIds(userId)
        chirps <- chirpService.getLiveChirps.invoke(LiveChirpsRequest(userIds))
      } yield chirps
  }

  override def getHistoricalActivityStream(userId: String): ServiceCall[HistoricalActivityStreamReq, Source[Chirp, NotUsed]] = {
    req =>
      for {
        userIds <- userAndFriendIds(userId)
        historicalChirpsRequest = HistoricalChirpsRequest(req.fromTime, userIds)
        chirps <- chirpService.getHistoricalChirps.invoke(historicalChirpsRequest)
      } yield chirps
  }

  private def userAndFriendIds(userId: String): Future[Seq[String]] =
    friendService.getUser(userId).invoke().map(user => {
      userId +: user.friends
    })
}
