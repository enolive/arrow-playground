package resources

import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.parZip
import arrow.fx.coroutines.resourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty

class ResourcesTest : DescribeSpec({
  describe("Resources") {
    resourceScope {
      val service = parZip(
        { userProcessor() },
        { dataSource() }) { userProcessor, dataSource ->
        Service(dataSource, userProcessor)
      }

      val data = service.processData()

      data.shouldNotBeEmpty()
    }
  }
})

class Service(val db: DataSource, val userProcessor: UserProcessor) {
  suspend fun processData(): List<String> {
    log.info { "processing data" }
    return listOf("Hello", "World")
  }
}

class UserProcessor {
  fun start(): Unit = log.info { "Creating UserProcessor" }
  fun shutdown(): Unit = log.info { "Shutting down UserProcessor" }
}

class DataSource {
  fun connect(): Unit = log.info { "Connecting dataSource" }
  fun close(): Unit = log.info { "Closed dataSource" }
}

suspend fun ResourceScope.userProcessor(): UserProcessor =
  install({ UserProcessor().also { it.start() } }) { p, _ -> p.shutdown() }

suspend fun ResourceScope.dataSource(): DataSource =
  install({ DataSource().also { it.connect() } }) { ds, _ -> ds.close() }

private val log = KotlinLogging.logger {}