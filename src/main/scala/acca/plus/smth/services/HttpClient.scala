package acca.plus.smth.services

import acca.plus.smth.AppConfig
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import com.google.inject.Singleton
import javax.inject.Inject
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


trait HttpClient {
  def doGetRequest(uri: String): Task[HttpResponse]
}

@Singleton
class DefaultHttpClient @Inject()(config: AppConfig)(implicit sys: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer, global: Scheduler) extends HttpClient with Logging {


  val host = config.host
  val login = config.login
  val password = config.password

  val orig = ConnectionPoolSettings(sys.settings.config).copy(idleTimeout = 15 minutes)
  val clientSettings = orig.connectionSettings.withIdleTimeout(15 minutes)
  val settings = orig.copy(connectionSettings = clientSettings)

  val http = Http()

  def doGetRequest(uri: String): Task[HttpResponse] = {
    val request = HttpRequest(method = HttpMethods.GET, uri)
    Task.deferFutureAction { implicit scheduler =>
      http.singleRequest(request)
    }
  }

}
