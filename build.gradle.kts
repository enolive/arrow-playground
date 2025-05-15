plugins {
  kotlin("jvm") version "2.1.20"
}

group = "de.welcz"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  val kotestVersion = "5.9.1"
  val arrowVersion = "2.1.0"

  implementation("io.arrow-kt:arrow-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-resilience:$arrowVersion")
  implementation("io.arrow-kt:arrow-atomic:$arrowVersion")
  implementation("io.github.oshai:kotlin-logging:7.0.7")
  implementation("ch.qos.logback:logback-classic:1.5.18")

  testImplementation(kotlin("test"))
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}