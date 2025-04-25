package resilience

import arrow.resilience.CircuitBreaker
import arrow.resilience.Schedule
import arrow.resilience.retryOrElseEither
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

class CircuitBreakerTest : DescribeSpec({
  lateinit var circuitBreaker: CircuitBreaker
  val maxFailures = 5

  beforeAny {
    circuitBreaker = CircuitBreaker.Companion(
      resetTimeout = 2.seconds,
      openingStrategy = CircuitBreaker.OpeningStrategy.Count(maxFailures),
    )
  }

  describe("Circuit Breaker") {
    it("works") {
      val result1 = circuitBreaker.protectOrThrow { 42 }

      result1 shouldBe 42
      circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
    }

    it("goes into open state") {
      repeat(maxFailures + 1) {
        shouldThrowAny {
          circuitBreaker.protectOrThrow { error("BAM!") }
        }.shouldHaveMessage("BAM!")
      }

      shouldThrow<CircuitBreaker.ExecutionRejected> {
        circuitBreaker.protectOrThrow { 42 }
      }.message.shouldBeNull()
      circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Open>()
    }

    it("can be combined with a schedule") {
      val schedule = (Schedule.Companion.recurs<Throwable>(maxFailures.toLong()) zipLeft Schedule.Companion.exponential(base = 1.seconds))
        .jittered()
        .log { error, attempts -> logger.warn(error) { "failed at $attempts attempt" } }

      val failure = schedule.retryOrElseEither(
        { circuitBreaker.protectOrThrow { error("BAM!") } },
        { error, attempts -> RetriesExhaustedError(attempts) }
      )

      failure shouldBeLeft RetriesExhaustedError(5)

      val success = schedule.retryOrElseEither(
        { circuitBreaker.protectOrThrow { 42 } },
        { _, _ -> fail("should be unreachable") }
      )

      success shouldBeRight 42
    }
  }
})

data class RetriesExhaustedError(val attempts: Long)

private val logger = KotlinLogging.logger {}