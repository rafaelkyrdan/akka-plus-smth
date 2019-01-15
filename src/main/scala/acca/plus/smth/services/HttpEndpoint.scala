package acca.plus.smth.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives.{ complete, get, path, _ }
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.{ ExecutionContext, Future }

class HttpEndpoint()(implicit sys: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer, global: Scheduler) {

  var bindingFuture: Future[Http.ServerBinding] = _

  val jsonSupport = JsonSupport()(ec, mat)

  import jsonSupport._

  def start(host: String, port: Int): Unit = {
    bindingFuture = Http().bindAndHandle(route, host, port)
    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  }

  def stop(): Unit = {
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => sys.terminate()) // and shutdown when done
  }

  val route: Route =
    path("") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            <h1>Say hello to akka-http</h1>
            <ul>
              <li><a href="runs">f_fct_runs</a></li>
              <li><a href="delays">s_stg_delays</a></li>
            </ul>
          """.stripMargin))
      }
    } ~ path("runs") {
      get {
        complete(200 -> "get runs")
      }
    } ~ path("delays") {
      get {
        complete(200 -> "get delays")
      }
    }
}
