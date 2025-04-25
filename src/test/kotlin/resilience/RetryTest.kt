package resilience

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.resilience.Schedule
import arrow.resilience.retryOrElseEither
import arrow.resilience.retryRaise
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import kotlin.time.Duration.Companion.seconds

class RetryTest : DescribeSpec({
  describe("Retry") {
    val policy = Schedule.Companion
      .forever<BaseError>()
      .log { error, attempts -> logger.warn { "$error on $attempts attempt" } }

    it("works sometimes :-p") {
      val result = either {
        mightFail()
      }

      result shouldBeRight 42
    }

    it("works always") {
      val result = policy.retryRaise { mightFail() }

      result shouldBeRight 42
    }

    it("works only on specific errors") {
      val conditionalPolicy = policy.doWhile { error, _ -> error is WtfError }

      val result = conditionalPolicy.retryRaise { mightFail() }

      result shouldBeRight 42
    }

    it("works mostly with max retries, jitter and backoff") {
      val realWorldPolicy =
        (Schedule.Companion.recurs<BaseError>(5) and Schedule.Companion.exponential(1.seconds)).jittered()
          .log { error, output -> logger.warn { "$error with $output" } }

      val result = realWorldPolicy.retryRaise { mightFail() }

      result shouldBeRight 42
    }

    it("works for classical exceptions") {
      val exceptionPolicy = Schedule.Companion.recurs<Throwable>(5)
        .log { error, attempts -> logger.warn { "$error, attempt $attempts" } }

      val result = exceptionPolicy
        .retryOrElseEither(
          {
            throw RuntimeException("BAM!")
          },
          { err, output -> WtfError }
        )

      result shouldBeLeft WtfError
    }
  }
})

private fun Raise<BaseError>.mightFail(): Int {
  val random = (1..3).random()
  if (random == 1) {
    raise(WtfError)
  }
  if (random == 2) {
    raise(ThisIsFineError)
  }
  return 42
}

sealed class BaseError(val message: String)
data object WtfError : BaseError("WTF???")
data object ThisIsFineError : BaseError("This is a Fine")

private val logger = KotlinLogging.logger {}