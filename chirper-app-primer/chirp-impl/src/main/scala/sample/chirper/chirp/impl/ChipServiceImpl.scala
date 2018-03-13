/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.impl

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.ServiceCall

import com.lightbend.lagom.scaladsl.pubsub.{PubSubRef, PubSubRegistry, TopicId}
import org.slf4j.LoggerFactory
import sample.chirper.chirp.api.{Chirp, ChirpService, HistoricalChirpsRequest, LiveChirpsRequest}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class ChirpServiceImpl (topics: PubSubRegistry)(implicit ex: ExecutionContext) extends ChirpService {

  private val RecentChirpNumber: Int = 10

  private val log = LoggerFactory.getLogger(classOf[ChirpServiceImpl])

  /**
    * By default sorted with time
    */
  private var allChirps = List[Chirp]()

  override def addChirp(userId: String): ServiceCall[Chirp, Done] = {
    chirp => {
      if (userId != chirp.userId)
        throw new IllegalArgumentException(s"UserId $userId did not match userId in $chirp")

      val topic: PubSubRef[Chirp] = topics.refFor(TopicId(userId))
      topic.publish(chirp)

      this.synchronized {
        allChirps = chirp :: allChirps
      }
      Future.successful(Done)
    }
  }

  override def getLiveChirps: ServiceCall[LiveChirpsRequest, Source[Chirp, NotUsed]] = {
    req => {
      val chirps = getHistoricalChirpsOrderedByTime(req.userIds, RecentChirpNumber)
      val liveChirpSources: Seq[Source[Chirp, NotUsed]] = for(userId <- req.userIds) yield {
        val topic: PubSubRef[Chirp] = topics.refFor(TopicId(userId))
        topic.subscriber
      }

      val users = req.userIds.toSet
      val allUsersLiveChirpSource: Source[Chirp, NotUsed] = Source(liveChirpSources).flatMapMerge(liveChirpSources.size, x => x)
        .filter(chirp => users(chirp.userId))

      // We currently ignore the fact that it is possible to get duplicate chirps
      // from the recent and the topic. That can be solved with a de-duplication stage.
      Future.successful(Source(chirps).concat(allUsersLiveChirpSource))
    }
  }

  override def getHistoricalChirps: ServiceCall[HistoricalChirpsRequest, Source[Chirp, NotUsed]] = {
    req => {
      val userIds = req.userIds
      val chirps = getHistoricalChirpsOrderedByTime(userIds)

      // Chirps from one user are ordered by timestamp, but chirps from different
      // users are not ordered. That can be improved by implementing a smarter
      // merge that takes the timestamps into account.
      Future.successful(Source(chirps))
    }
  }


  private def getHistoricalChirpsOrderedByTime(userIds: Seq[String], limit: Int = -1): Seq[Chirp] = {
    def getChirps(userId: String): Seq[Chirp] = {
      for {
        row <- allChirps
        if row.userId == userId
      } yield row
    }
    val userChirps = userIds.flatMap(getChirps).sorted.reverse

    if (limit > 0) {
      userChirps.take(limit)
    } else {
      userChirps
    }
  }

}
