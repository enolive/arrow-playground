package resilience

import arrow.atomic.AtomicInt
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.resilience.Saga
import arrow.resilience.saga
import arrow.resilience.transact
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage

class SagaTest : DescribeSpec({
  describe("Saga Pattern") {
    it("works with eithers") {
      val result = either { raiseTransaction().transact() }

      result shouldBeLeft "Boom"
      Counter.value.get() shouldBe INITIAL_VALUE
    }

    it("works for exceptions") {
      shouldThrowAny {
        exceptionalTransaction().transact()
      }.shouldHaveMessage("Boom")
      Counter.value.get() shouldBe INITIAL_VALUE
    }
  }
})

private fun Raise<String>.raiseTransaction() = saga {
  saga({
    log.info { "performing business stuff" }
    Counter.increment()
  }) {
    log.warn { "rolling back business stuff" }
    Counter.decrement()
  }
  saga({
    log.info { "performing even more business stuff" }
    Counter.increment()
  }) {
    log.warn { "rolling back even more business stuff" }
    Counter.decrement()
  }
  saga({
    log.info { "doing something stupid" }
    raise("Boom")
  }) {
    log.warn { "rolling back something stupid" }
  }
  Counter.value.get()
}

private fun exceptionalTransaction(): Saga<Int> = saga {
  saga({
    log.info { "performing business stuff" }
    Counter.increment()
  }) {
    log.warn { "rolling back business stuff" }
    Counter.decrement()
  }
  saga({
    log.info { "performing even more business stuff" }
    Counter.increment()
  }) {
    log.warn { "rolling back even more business stuff" }
    Counter.decrement()
  }
  saga({
    log.info { "doing something stupid" }
    error("Boom")
  }) {
    log.warn { "rolling back something stupid" }
  }
  Counter.value.get()
}


const val INITIAL_VALUE = 1

object Counter {
  val value = AtomicInt(INITIAL_VALUE)

  fun increment() {
    value.incrementAndGet()
  }

  fun decrement() {
    value.decrementAndGet()
  }
}

private val log = KotlinLogging.logger {}
