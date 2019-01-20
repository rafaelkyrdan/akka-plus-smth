package acca.plus.smth

import com.google.inject.{ AbstractModule, Module }
import net.codingwell.scalaguice.ScalaModule
import javax.inject.Provider

class TestApp(configOverride: Map[String, String] = Map()) extends App(configOverride) {

}

object TestApp {
  def module[T: Manifest](instance: T) = {
    new AbstractModule with ScalaModule {
      override def configure(): Unit = bind[T].toInstance(instance)
    }
  }

  def moduleProvider[T: Manifest, P <: Provider[_ <: T] : Manifest] = {
    new AbstractModule with ScalaModule {
      override def configure(): Unit = bind[T].toProvider[P]
    }
  }

  def singletonModuleProvider[T: Manifest, P <: Provider[_ <: T] : Manifest] = {
    new AbstractModule with ScalaModule {
      override def configure(): Unit = bind[T].toProvider[P].asEagerSingleton()
    }
  }
}
