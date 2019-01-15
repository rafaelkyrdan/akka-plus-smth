package acca.plus.smth

import com.google.inject.AbstractModule
import com.typesafe.config.{ Config, ConfigFactory }
import net.codingwell.scalaguice.ScalaModule
import scala.collection.JavaConverters._

class ConfigModule(configOverride: Map[String, _ <: Object] = Map()) extends AbstractModule with ScalaModule {

  override def configure() {
    val config = ConfigFactory.parseMap(configOverride.asJava).withFallback(ConfigFactory.load())
    bind[Config].toInstance(config)
  }
}
