package resilience

import arrow.core.raise.either
import arrow.resilience.Schedule
import arrow.resilience.retry
import arrow.resilience.retryOrElseEither
import arrow.resilience.retryRaise
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class RetryTest : DescribeSpec({
  describe("Retry") {
    val policy = Schedule
      .forever<BaseError>()
      .log { error, attempts -> log.warn { "$error on $attempts attempt" } }

    it("works never") {
      val result = either {
        raise(WtfError)
      }

      result shouldBeLeft WtfError
    }

    it("works always") {
      var count = 0
      val result = policy.retryRaise {
        if (count++ < 10) raise(WtfError)
        42
      }

      result shouldBeRight 42
    }

    it("works only on specific errors") {
      val conditionalPolicy = policy.doWhile { error, _ -> error is WtfError }
      var count = 0

      val result = conditionalPolicy.retryRaise {
        if (count++ < 5) raise(WtfError)
        if (count++ < 10) raise(ThisIsFineError)
        42
      }

      result shouldBeLeft ThisIsFineError
    }

    it("works with real world retry logic") {
      val realWorldPolicy =
        (Schedule.recurs<BaseError>(5) and Schedule.exponential(1.seconds)).jittered()
          .log { error, output -> log.warn { "$error with $output" } }
      var count = 0

      val result = realWorldPolicy.retryRaise {
        count++
        if (count > 3) {
          return@retryRaise 42
        }
        if (count % 2 == 0) raise(ThisIsFineError) else raise(WtfError)
      }

      result shouldBeRight 42
    }

    it("works for classical exceptions") {
      val exceptionPolicy = Schedule.recurs<Throwable>(5)
        .log { error, attempts -> log.warn { "$error, attempt $attempts" } }
      var count = 0

      val result = exceptionPolicy.retry {
        if (count++ < 5) error("BAM!")
        42
      }

      result shouldBe 42
    }

    it("works for classical exceptions turned into either") {
      val exceptionPolicy = Schedule.recurs<Throwable>(5)
        .log { error, attempts -> log.warn { "$error, attempt $attempts" } }
      var count = 0

      val result = exceptionPolicy
        .retryOrElseEither(
          {
            if (count++ < 10) error("BAM!")
            42
          },
          { err, output -> WtfError }
        )

      result shouldBeLeft WtfError
    }
  }
})

sealed class BaseError(val message: String)
data object WtfError : BaseError("WTF???")
data object ThisIsFineError : BaseError("This is a Fine")

private val log = KotlinLogging.logger {}