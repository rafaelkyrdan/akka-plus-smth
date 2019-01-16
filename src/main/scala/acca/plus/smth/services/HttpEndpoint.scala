package acca.plus.smth.services

import acca.plus.smth.models.{ AddUserRequest, User }
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{ complete, get, path, _ }
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import monix.eval.Task
import monix.execution.Scheduler
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class HttpEndpoint(userRepo: UserRepository)(implicit sys: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer, global: Scheduler) {

  var bindingFuture: Future[Http.ServerBinding] = _

  val jsonSupport = JsonSupport()(ec, mat)

  import jsonSupport._

  lazy val http = Http()

  def start(host: String, port: Int): Unit = {
    bindingFuture = http.bindAndHandle(route, host, port)
    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  }

  def stop(): Unit = {
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => sys.terminate()) // and shutdown when done
  }

  def doGetRequest(uri: String): Task[HttpResponse] = {
    val request = HttpRequest(method = HttpMethods.GET, uri)
    Task.fromFuture {
      http.singleRequest(request)
    }
  }

  val route: Route =
    path("") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            <h1>Say hello to akka-http</h1>
            <ul>
              <li><a href="users">only post</a></li>
              <li><a href="users/{email}">get and delete</a></li>
            </ul>
          """.stripMargin))
      }
    } ~ path("users") {
      post {
        entity(as[AddUserRequest]) { addUserRequest: AddUserRequest =>
          val future = (
            for {
              response <- doGetRequest(s"https://reqres.in/api/users/${addUserRequest.id}")
              res <-{
                if (response.status.isSuccess()) {
                  for {
                    userResponse <- Task.fromFuture {
                      response.entity.toStrict(2.seconds).map { e => e.data.utf8String } map { jsonStr =>
                        UserResponseJsonFormat.read(jsonStr.parseJson)
                      }
                    }
                    user = User(addUserRequest.email, userResponse.id, userResponse.firstName, userResponse.lastName)
                    _ <- userRepo.writeUsers(Seq(user))
                  } yield Map(StatusCodes.OK -> s"User with email: ${addUserRequest.email} has been added")
                } else {
                  Task.now(
                    Map(StatusCodes.NotFound -> s"There is no user with email: ${addUserRequest.email}")
                  )
                }
              }
            } yield res ).runToFuture
          onSuccess(future) {
            case result => complete(result.head)
          }
        }
      }
    } ~ path("users" / Segment) { email =>
      get {
        onSuccess(userRepo.getUsersByEmail(email).runToFuture) {
          case Some(user) => complete(jsonSupport.UserJsonFormat.write(user)) // should work implicitly
          case _ =>  complete(StatusCodes.NotFound -> s"There is no user with email: $email")
        }
      } ~ delete {
        val future = (for {
          userOpt <- userRepo.getUsersByEmail(email)
          res <- {
            val task: Task[Map[StatusCode, String]] = if (userOpt.exists(_.email == email)) {
              for {
                _ <- userRepo.deleteUser(email)
              } yield Map(StatusCodes.OK -> s"User with email: $email has been removed")
            } else {
              Task.now(
                Map(StatusCodes.NotFound -> s"There is no user with email: $email")
              )
            }
            task
          }
        } yield res).runToFuture

        onSuccess(future) {
          case response => complete(response.head)
        }
      }
    }
}
