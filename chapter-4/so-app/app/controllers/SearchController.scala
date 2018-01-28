package controllers

import javax.inject.{Inject, Singleton}

import com.microservices.auth.ResponseObj
import com.microservices.search.{SearchFilter, StackOverflowSearchResult}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import service.SearchService
import users.Contexts

import scala.collection.immutable.Seq
import scala.concurrent.Future

@Singleton
class SearchController @Inject()(service: SearchService, context: Contexts, cc: ControllerComponents) extends AbstractController(cc) {

  import context.cpuLookup

  def searchPost = Action.async(parse.json) { implicit request =>
    request.body.validate[SearchFilter] match {
      case filter: JsSuccess[SearchFilter] =>
        search(SearchFilter(filter.get.location, filter.get.tag))
          .map(results => Ok(Json.toJson(results)))
      case _: JsError => Future.successful(BadRequest(s"Not a valid input: $request.body"))
    }
  }

  def searchGet(location: String, tag: String) = Action.async { implicit request =>
    search(SearchFilter(Option(location), Option(tag)))
      .map(results => Ok(Json.toJson(results)))
  }

  private def search(filter: SearchFilter): Future[Seq[StackOverflowSearchResult]] = {
    service.searchFlatten(filter)
  }

}
