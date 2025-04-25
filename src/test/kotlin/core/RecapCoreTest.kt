package core

import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RecapCoreTest : DescribeSpec({
  describe("Core Functionality for working with typed errors") {
    describe("succeed or fail on first error") {
      it("fails") {
        val result = validateShortCircuit("", -1)

        result shouldBeLeft NameMustNotBeEmpty
      }

      it("succeeds") {
        val result = validateShortCircuit("Tyler Durden", 30)

        result.shouldBeRight().apply {
          name shouldBe "Tyler Durden"
          age shouldBe 30
        }
      }
    }

    describe("succeed or fail accumulating all errors") {
      it("fails") {
        val result = validateAccumulating("", -1)

        result shouldBeLeft nonEmptyListOf(NameMustNotBeEmpty, AgeMustBePositive)
      }

      it("succeeds") {
        val result = validateAccumulating("Tyler Durden", 30)

        result.shouldBeRight().apply {
          name shouldBe "Tyler Durden"
          age shouldBe 30
        }
      }
    }
  }
})

private fun validateShortCircuit(name: String, age: Int) = either {
  ensure(name.isNotEmpty()) { NameMustNotBeEmpty }
  ensure(age >= 0) { AgeMustBePositive }
  Person(name, age)
}

private fun validateAccumulating(name: String, age: Int) = either {
  zipOrAccumulate(
    { ensure(name.isNotEmpty()) { NameMustNotBeEmpty } },
    { ensure(age >= 0) { AgeMustBePositive } },
    { _, _ -> Person(name, age) }
  )
}

data class Person(val name: String, val age: Int)

sealed interface ValidationError
data object NameMustNotBeEmpty : ValidationError
data object AgeMustBePositive : ValidationError