package tokens

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.microservices.auth.Token
import utils.Contexts

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}


@Singleton
class TokenService @Inject()(contexts: Contexts, tokensDao: TokenDao) {

  implicit val executionContext = contexts.dbLookup

  /**
    * Creates a token based on the key provided. If there was already a token generated for the key and is valid, then the same token is returned
    * Else a new token is generated and returned
    * @param key key for example user email
    * @return
    */
  def createToken(key: String)(implicit exec:ExecutionContext): Future[Token] = {
    tokensDao.getTokenFromkey(key).flatMap {
      case Some(token) =>
        if(token.validTill <= System.currentTimeMillis()){
          dropToken(token.tokenStr)
          insertNewToken(key)
        } else{
          Future(token)
        }
      case None =>
        insertNewToken(key)
    }
  }

  /**
    * verifies if its a valid token. Returns a future completed with token if so. Else the returned future completes with an exception
    */
  def authenticateToken(token: String, refresh:Boolean)(implicit exec:ExecutionContext): Future[Try[Token]] = {
    tokensDao.getToken(token).map {
      case Some(t) =>
        if (t.validTill < System.currentTimeMillis())
          throw new IllegalArgumentException("Token expired.")
        else {
          if(refresh) {
            val max = maxTTL
            tokensDao.updateTTL(token, max)
            Success(Token(t.tokenStr, max, t.key))
          } else Success(t)
        }
      case None => throw new IllegalArgumentException("Not a valid Token.")
    }
  }


  def dropToken(tokenStr: String)={
    tokensDao.deleteToken(tokenStr)
  }

  private def insertNewToken(key: String): Future[Token] = {
    val newToken = generateToken(key)
    tokensDao.createToken(newToken).map(_ => newToken)
  }

  private def generateToken(key: String) = Token(generateTokenStr, maxTTL, key)

  private def generateTokenStr: String = UUID.randomUUID().toString
  private def maxTTL = System.currentTimeMillis() + contexts.tokenTTL

}
