package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class RestAuth @Inject() (parser: BodyParsers.Default, config: Configuration)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  val username = config.get[String]("measurement_server.username")
  val password = config.get[String]("measurement_server.password")
    
  override def invokeBlock[A](request: Request[A], action: (Request[A]) => Future[Result]) = {
    val submittedCredentials: Option[List[String]] = for {
      authHeader <- request.headers.get("Authorization")
      parts <- authHeader.split(' ').drop(1).headOption
    } yield new String(org.apache.commons.codec.binary.Base64.decodeBase64(parts.getBytes)).split(':').toList

    submittedCredentials.collect {
      case u :: p :: Nil if u == username && p == password =>
    }.map(_ => action(request)).getOrElse {
      Future.successful(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured Area""""))
    }
  }
}