package acca.plus.smth

import com.typesafe.config.Config
import javax.inject.{ Inject, Singleton }

@Singleton
class AppConfig @Inject()(config: Config) {
  val applicationName = config.getString("applicationName")

  val host = config.getString("host")
  val port = config.getInt("port")
  val login = config.getString("login")
  val password = config.getString("password")

}
