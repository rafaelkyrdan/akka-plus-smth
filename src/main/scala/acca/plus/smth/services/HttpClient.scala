package acca.plus.smth.services

import java.time.Clock

import akka.http.javadsl.model.HttpEntities
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.google.inject.Singleton
import javax.inject.Inject
import monix.eval.Task
import acca.plus.smth.AppConfig

import scala.concurrent.duration._


trait HttpClient {
  def doLogin(): Task[HttpResponse]

  def submitTheFile(cookie: String, jsonString: String, path: String, index: Int): Task[StatusCode]

  def getCookie(headers: Seq[HttpHeader]): String

}

@Singleton
class DefaultHttpClient @Inject()(clock: Clock, config: AppConfig) extends HttpClient with Logging {


  val host = config.host
  val login = config.login
  val password = config.password

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer

  // TODO: move it to app and send them as params
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val orig = ConnectionPoolSettings(system.settings.config).copy(idleTimeout = 15 minutes)
  val clientSettings = orig.connectionSettings.withIdleTimeout(15 minutes)
  val settings = orig.copy(connectionSettings = clientSettings)

  val http = Http()

  def doLogin(): Task[HttpResponse] = {
    val request = HttpRequest(method = HttpMethods.POST, uri = s"$host/j_spring_security_check", entity = FormData(
      "j_username" -> login,
      "j_password" -> password
    ).toEntity)

    Task.deferFutureAction { implicit scheduler =>
      http.singleRequest(request)
    }
  }

  def submitTheFile(cookie: String, jsonString: String, path: String, index: Int): Task[StatusCode] = {
    val parsed = cookie.split("=").toList
    val cookieHeader = Cookie(parsed.head, parsed.last)
    val request = HttpRequest(method = HttpMethods.POST, uri = s"$host/api/atlas/v2/$path", headers = List(cookieHeader)).withEntity(HttpEntities.create(
      ContentTypes.`application/json`,
      jsonString
    ))

    Task.deferFutureAction { implicit scheduler =>
      logger.info(s"uploading the file # $index")
      http.singleRequest(request, settings = settings).map(response => {
        logger.info(s"received the response for file # $index with status: ${response.status}")
        response.discardEntityBytes()
        response.status
      })
    }
  }

  def getCookie(headers: Seq[HttpHeader]): String = {
    val header = headers.filter(_.name().toLowerCase == "set-cookie").toList
    val cookie = header.flatMap(_.value().split(";").toList)
    cookie.head
  }

}
