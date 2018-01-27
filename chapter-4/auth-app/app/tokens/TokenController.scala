package tokens

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import utils.Contexts

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class TokenController @Inject()(contexts: Contexts, tokenService: TokenService, cc: ControllerComponents) extends AbstractController(cc) {
  implicit val executionContext = contexts.cpuLookup

  /**
    * Refreshes the token for a user with a new token.
    *
    * @param token the token of the user
    * @return
    */
  /*
  curl -X GET http://localhost:5001/v1/tokens/refresh/677678f7-5dc9-4236-a254-c067b0662e8c
   */
  def refreshToken(token: String) = Action.async {
    authenticateAndRefresh(token)
  }

  /**
    * Authenticates a user based on the token provided. Returns the token object if success.
    * Else returns a BadRequest
    *
    * @param token
    * @return
    */
  /*
  Sample call (the token would need to be replaced with your generated token)
  curl -X GET http://localhost:5001/v1/tokens/authenticate/677678f7-5dc9-4236-a254-c067b0662e8c
   */
  def authenticate(token: String) = Action.async {
    authenticateAndRefresh(token)
  }

  private def authenticateAndRefresh(token: String): Future[Result] =
    tokenService.authenticateToken(token, true)
      .map({
        case Success(refreshedToken) => Ok(Json.toJson(refreshedToken))
        case Failure(e) => BadRequest(Json.toJson(e.getMessage))
      })
}
