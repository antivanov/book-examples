package com.microservices.search

import play.api.libs.json.Json

abstract class SearchUserResult{
  require(score >= 0 && score <= 1, s"score must be in range of [0-1]. passed: $score")

  /**
    * value in range of 0-1 absolute
    */
  def score: Float
  def tag: String
  def location: String
}

case class SearchFilter(location: Option[String], tag: Option[String])
object SearchFilter{
  implicit val format = Json.format[SearchFilter]
}

case class StackOverflowUser(id:Long, name:String, accountId: Long, aboutMe:String, link:String="#", location:String)
object StackOverflowUser{
  implicit val stackOverflowUserJSON = Json.format[StackOverflowUser]
}

case class StackOverflowTag(id:Int, name:String)

object StackOverflowTag{
  implicit val stackOverflowTagJSON = Json.format[StackOverflowTag]
}


case class StackOverflowUserScore(user:StackOverflowUser, map: Map[StackOverflowTag, Int])

case class StackOverflowSearchResult(override val score:Float, stackOverflowTag: StackOverflowTag, user: StackOverflowUser) extends SearchUserResult {
  override val location = user.location
  override val tag = stackOverflowTag.name
}

object StackOverflowSearchResult{
  implicit val stackOverflowSearchResultJSON = Json.format[StackOverflowSearchResult]
}

