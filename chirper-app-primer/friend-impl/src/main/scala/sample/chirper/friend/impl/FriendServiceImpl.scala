/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import sample.chirper.friend.api.{CreateUser, FriendId, FriendService, User}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import scala.collection.JavaConverters._

class FriendServiceImpl()(implicit ec: ExecutionContext) extends FriendService {

  val userMap = new ConcurrentHashMap[String, User]()

  val friendsMap = new ConcurrentHashMap[String, ConcurrentLinkedQueue[User]]()

  override def getUser(userId: String): ServiceCall[NotUsed, User] = {
    _ =>
      val user = userMap.get(userId)
      if (user == null)
        throw NotFound(s"user $userId not found")
      else {
        Future.successful(getUser(user.userId, user.name))
      }
  }

  override def createUser(): ServiceCall[CreateUser, Done] = {
    request =>
      this.synchronized {
        val alreadyExists = userMap.get(request.userId)
        if (alreadyExists != null) {
          throw NotFound(s"user $request already exists")
        }

        val user = User(request)
        userMap.put(request.userId, user)
        val friends = new ConcurrentLinkedQueue[User]()

        friendsMap.put(user.userId, friends)
        Future.successful(Done)
      }
  }

  override def addFriend(userId: String): ServiceCall[FriendId, Done] = {
    request =>
      val user = userMap.get(userId)

      if (user == null)
        throw NotFound(s"user $userId not found")
      else {
        val friendsList = friendsMap.get(userId)
        val friend = userMap.get(request.friendId)
        friendsList.add(friend)
        Future.successful(Done)
      }
  }


  override def getFollowers(userId: String): ServiceCall[NotUsed, Seq[String]] = {
    request => {
      val user = userMap.get(userId)
      if (user == null)
        throw NotFound(s"user $userId not found")
      else {
        Future.successful(getFollowerUserIds(userId))
      }
    }
  }

  private def getUser(userId: String, name: String): User =
    User(userId, name, getFollowerUserIds(userId))

  private def getFollowerUserIds(userId: String): Seq[String] =
    friendsMap.get(userId).asScala.toList.map(_.userId)
}