package acca.plus.smth

import java.time.{ Clock, Instant, ZoneId }
import java.util.concurrent.TimeUnit

import acca.plus.smth.services.{ Logging, PostgresAdapter }
import com.typesafe.config.Config
import org.mockito.Mockito.withSettings
import org.mockito.internal.MockitoCore
import org.specs2
import org.specs2.execute._
import org.specs2.matcher.{ DataTables, JsonNumber, JsonType, MatchResultCombinators, Matcher, MatcherMacros, MustMatchers }
import org.specs2.mock.Mockito
import org.specs2.specification.{ AfterEach, AroundEach, BeforeAll }
import org.specs2.specification.core.SpecStructure
import org.specs2.specification.dsl.FragmentsDsl
import org.specs2.specification.dsl.mutable.TagDsl
import org.mockito.{ Mockito => OriginalMockito }

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.Try

/**
 * @param skipSpec if it's `true` then all examples within a specification will be skipped
 */
abstract class UnitTest(skipSpec: Boolean = false) extends specs2.mutable.Spec with Mockito with DataTables
  with FragmentsDsl with TagDsl with Logging with BeforeAll with PendingUntilFixed with MatchResultCombinators
  with AroundTimeout {

  import org.specs2.matcher.MatchersImplicits._

  override def is: SpecStructure = {
    if (skipSpec) {
      skipAll ^ super.is
    } else {
      super.is
    }
  }

  override def beforeAll(): Unit = {
    logger.info(s"Starting test class ${getClass.getCanonicalName}")
  }

  def JsonTypeMatcherDouble(expected: Double): Matcher[JsonType] = (actual: JsonType) => actual match {
    case JsonNumber(n) => (n.toDouble == expected.toDouble, s"$n is not equal to $expected")
    case other => (false, s"not a String: $other")
  }

  def clockUTC(): Clock = Clock.systemUTC()

  def fixedClock(sec: Int) = Clock.fixed(Instant.ofEpochSecond(sec), ZoneId.of("GMT"))

  def fixedInstant(sec: Int) = fixedClock(sec).instant

  def mockSerializable[T: ClassTag]: T = {
    mock[T](withSettings().serializable())
  }

  def spySerializable[T](obj: T): T = {
    val field = classOf[OriginalMockito].getDeclaredField("MOCKITO_CORE")
    field.setAccessible(true)
    val mockitoCore: MockitoCore = field.get(null).asInstanceOf[MockitoCore]

    mockitoCore.mock(
      obj.getClass.asInstanceOf[Class[T]],
      withSettings()
        .spiedInstance(obj)
        .defaultAnswer(OriginalMockito.CALLS_REAL_METHODS)
        .serializable()
    )
  }

  implicit def unitAsResult: AsResult[Unit] = new AsResult[Unit] {
    def asResult(r: => Unit) = ResultExecution.execute(r)(_ => Success())
  }
}

/**
 * Mix this trait to use macros based Matchers like [[MatcherMacros.matchA]].
 */
trait MacrosMatcher extends MatcherMacros with MustMatchers

abstract class IntegrationTest(skipSpec: Boolean = false) extends UnitTest(skipSpec) with AroundEach with AfterEach {


  /**
   * When there are multiple test examples per specification, they run
   * in parallel each in a thread. This variable holds an instance of test
   * app per test example thread.
   */
  private val threadLocalApp = new ThreadLocal[TestApp]

  val runningOutsideSbt = new Throwable().getStackTrace.exists(_.getClassName.contains("JavaSpecs2Runner"))


  def app: TestApp = threadLocalApp.get

  def appClock(): Clock = app.injector.getInstance(classOf[Clock])

  override protected def after: Any = {
    Option(app).foreach { currentApp =>
      //      currentApp.stop()
      threadLocalApp.set(null)
    }
  }

  override protected def around[R](r: => R)(implicit evidence$1: AsResult[R]): Result = {
    if (runningOutsideSbt) {
      AsResult(r)
    } else {
      AsResult(eventually(retries = 2, sleep = FiniteDuration(10, TimeUnit.MILLISECONDS))(r))
    }
  }

  /** Set the test app for the current test example and calls its syncStart method */
  def withApp(testApp: => TestApp): TestApp = {
    lazy val currentTestApp = testApp
    threadLocalApp.set(currentTestApp)
    Option(currentTestApp).foreach { currentTestApp =>
      val config = currentTestApp.injector.getInstance(classOf[Config])
      if (Try(config.getBoolean("doInitializeDb")).getOrElse(false)) {
        PostgresScope.initializeDb(currentTestApp.injector.getInstance(classOf[PostgresAdapter]))
        logger.info("Initializing test database")
      }

      if (Try(config.getBoolean("doSeedDb")).getOrElse(false)) {
        PostgresScope.seedDb(currentTestApp.injector.getInstance(classOf[PostgresAdapter]))
        logger.info("Seeding test database")
      }
    }
    currentTestApp
  }
}
