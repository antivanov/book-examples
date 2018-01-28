package service

import javax.inject.{Inject, Singleton}

import com.microservices.search.{StackOverflowSearchResult, SearchFilter, StackOverflowUserScore}
import dao.SearchDao
import play.api.Logger

import scala.collection.immutable.{Iterable, Seq}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchService @Inject()(dao: SearchDao) {

  private val log = Logger(getClass)

  def search(filter: SearchFilter)(implicit exec: ExecutionContext): Future[Iterable[StackOverflowUserScore]] = {
    dao.getUsers(filter.location, filter.tag)
  }

  def searchFlatten(filter: SearchFilter)(implicit exec: ExecutionContext): Future[Seq[StackOverflowSearchResult]] = {
    log.debug(s"Request for filter: $filter")

    search(filter).map(ans => {
      ans.toList.flatMap(x => x.map.map(tags => StackOverflowSearchResult(1, tags._1, x.user)))
    })
  }
}
