package concurrency

import arrow.fx.coroutines.await.ExperimentalAwaitAllApi
import arrow.fx.coroutines.await.awaitAll
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parZip
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@OptIn(ExperimentalAwaitAllApi::class)
class ConcurrencyTest : DescribeSpec({
  describe("higher level concurrency") {
    it("retrieves resources parallel with parZip") {
      val id = 42
      val user = parZip({
        getUserName(id)
      }, {
        getAvatar(id)
      }) { userName, avatar -> User(userName, avatar) }

      user.shouldNotBeNull()
    }

    it("retrieves resources parallel with awaitAll") {
      val id = 42
      val user = awaitAll {
        val userName = async { getUserName(id) }
        val avatar = async { getAvatar(id) }
        User(userName.await(), avatar.await())
      }

      user.shouldNotBeNull()
    }

    it("retrieves list of resources in parallel") {
      val userNames = getUserIds().parMap { getUserName(it) }

      userNames.shouldNotBeEmpty()
    }

    it("retrieves list of resources in parallel with limited concurrency") {
      val userNames = getUserIds().parMap(concurrency = 5) { getUserName(it) }

      userNames.shouldNotBeEmpty()
    }
  }
})

private suspend fun getAvatar(id: Int): ByteArray {
  measureTime {
    log.info { "getting avatar for $id..." }
    randomDelayToSimulateSlowApi()
  }.also { log.info { "successfully got avatar for $id in $it" } }
  return byteArrayOf(1, 2, 3, 4, 5)
}

private suspend fun getUserName(id: Int): String {
  measureTime {
    log.info { "getting user name for $id..." }
    randomDelayToSimulateSlowApi()
  }.also {
    log.info { "successfully got user name for $id in $it" }
  }
  return "Tyler-$id"
}

private fun getUserIds(): List<Int> {
  return (1..10).toList()
}

private suspend fun randomDelayToSimulateSlowApi() {
  val duration = Random.nextInt(1..5).seconds
  log.info { "will wait $duration" }
  delay(duration)
}

data class User(val userName: String, val avatar: ByteArray)

private val log = KotlinLogging.logger {}