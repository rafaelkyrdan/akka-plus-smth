package acca.plus.smth.services

import com.typesafe.scalalogging
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.slf4j.{ Logger => SlfLogger }
import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }

trait Logging {
  def logger: Logger = Logging.default
}

object Logging {
  val default = scalalogging.Logger(LoggerFactory.getLogger("default"))

  def silent(): Unit = setLogLevel(Level.OFF)

  def debugging(): Unit = setLogLevel(Level.DEBUG)

  def info(): Unit = setLogLevel(Level.INFO)

  private[this] def setLogLevel(level: Level): Unit = {
    LoggerFactory.getLogger(SlfLogger.ROOT_LOGGER_NAME) match {
      case root: LogbackLogger =>
        root.setLevel(level)
      case els =>
        els.warn(s"Ignored changing the log level to $level. It might be running in unit testing.")
    }
  }
}
