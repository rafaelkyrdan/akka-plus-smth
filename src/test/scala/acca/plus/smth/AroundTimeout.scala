package acca.plus.smth

import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{ AsResult, Failure }
import org.specs2.matcher.MustExpectable
import org.specs2.matcher.TerminationMatchers.terminate
import org.specs2.specification.core.Env
import org.specs2.specification.{ Around, Context, EachContext }

import scala.concurrent.duration._

/**
 * This interface used in setting a timeout for the whole test.
 * The current default timeout is 5 minutes, to override the default timeout override method "timeout"
 */
trait AroundTimeout extends EachContext {

  def timeout: Duration = AroundTimeout.defaultTimeout

  def context: Env => Context = { env: Env =>
    aroundTimeout(timeout)(env.executionEnv)
  }

  def aroundTimeout(to: Duration)(implicit ee: ExecutionEnv): Around =
    new Around {
      def around[T: AsResult](t: => T) = {
        lazy val result = t
        val checkEvery = 2.seconds
        val retries = math.max(1, (to.toSeconds / checkEvery.toSeconds).toInt)
        val termination = terminate(retries, checkEvery).orSkip(_ => "TIMEOUT: " + to)(MustExpectable.apply(result))

        if (!termination.toResult.isSkipped) AsResult(result)
        else Failure("test timed out")
      }
    }

}

object AroundTimeout {
  private val defaultTimeout = 8.minutes
}
