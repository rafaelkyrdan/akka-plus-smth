package acca.plus.smth

import acca.plus.smth.services.{ DefaultHttpClient, HttpEndpoint, PostgresAdapter, UserRepository }
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.util.Modules
import com.google.inject.{ Guice, Module }
import com.typesafe.config.Config
import javax.inject.Inject
// needed for the future flatMap/onComplete in the end, etc
import monix.execution.Scheduler.Implicits.global


class App @Inject()(configOverride: Map[String, String] = Map()) {

  val injector = Guice.createInjector(
    Modules.`override`(
      new ConfigModule(configOverride),
      new AppModule()
    ).`with`(Seq.empty[Module]: _*)
  )

  val config = injector.getInstance(classOf[Config])
  val appConfig = injector.getInstance(classOf[AppConfig])
  val postgresAdapter = new PostgresAdapter(config)
  val userRepo = new UserRepository(postgresAdapter)

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  //  implicit val executionContext = system.dispatcher

  val httpEndpoint = new HttpEndpoint(userRepo)
//  val httpClient = new DefaultHttpClient(appConfig)


  def start(): Unit = {
    // start http endpoint
    httpEndpoint.start(appConfig.host, appConfig.port)
  }

  def stop(): Unit = {
    httpEndpoint.stop()
  }
}
