package dao

import javax.inject.Singleton

import com.google.inject.Inject
import com.microservices.search.{StackOverflowTag, StackOverflowUser, StackOverflowUserScore}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.GetResult

import scala.collection.immutable.Iterable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


@Singleton
class SearchDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  implicit val getUserResult: GetResult[(StackOverflowUser, StackOverflowTag, Int)] = GetResult(r =>
    (StackOverflowUser(r.nextInt(), r.nextString(), r.nextInt(), r.nextString(), r.nextString(), r.nextString()),
      StackOverflowTag(r.nextInt(), r.nextString),
      r.nextInt()))


  def getUsers(location: Option[String], tag: Option[String])(implicit exec:ExecutionContext): Future[Iterable[StackOverflowUserScore]] = {

    val selectQ =
      """select a.id,a.name, a.so_account_id, a.about_me, a.so_link, a.location, c.id,c.name,b.points from so_user_info a
            join so_reputation b on b.user=a.id
            join so_tag c on b.tag=c.id
            """

    val allFuture = (location, tag) match {
      case (Some(loc), Some(tag)) =>
        db.run(sql"""#$selectQ
               where LOWER(a.location) = LOWER($loc)
               AND LOWER(c.name) = LOWER($tag)""".as[(StackOverflowUser, StackOverflowTag, Int)])
      case (Some(loc), None) =>
         db.run(sql"""#$selectQ
               where LOWER(a.location) = LOWER(${loc})""".as[(StackOverflowUser, StackOverflowTag, Int)])
      case (None, Some(tag)) =>
         db.run(sql"""#$selectQ
               where LOWER(c.name) = LOWER(${tag})""".as[(StackOverflowUser, StackOverflowTag, Int)])
      case (None, None) => db.run(sql"""#$selectQ""".as[(StackOverflowUser, StackOverflowTag, Int)])
    }

    allFuture.map(allUsers => {
      allUsers.groupBy({
        case (stackOverflowUser, stackOverflowTag, points) => stackOverflowUser
      }).map({
        case (stackOverflowUser, tagsAndPoints) => {
          val tagPoints = tagsAndPoints.map({
            case (_, stackOverflowTag, points) => (stackOverflowTag, points)
          }).toMap
          StackOverflowUserScore(stackOverflowUser, tagPoints)
        }
      })
    })

  }
}
