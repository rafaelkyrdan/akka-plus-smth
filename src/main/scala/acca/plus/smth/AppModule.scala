package acca.plus.smth

import java.time.Clock

import acca.plus.smth.services.{ DefaultHttpClient, HttpClient }
import com.google.inject.AbstractModule
import com.typesafe.config.{ Config, ConfigFactory }
import net.codingwell.scalaguice.ScalaModule


class AppModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    val config: Config = ConfigFactory.load("application")
    val serviceConfig = new AppConfig(config)
    bind[AppConfig].toInstance(serviceConfig)

    bind[Clock].toInstance(Clock.systemUTC())
    bind[HttpClient].to[DefaultHttpClient]
  }
}