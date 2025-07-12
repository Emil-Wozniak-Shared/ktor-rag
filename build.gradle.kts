import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    groovy
}

group = "pl"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"

    val isDevelopment: Boolean = project.ext.has("dev")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.cohort.ktor)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.line.webhook.plugin)
    implementation(libs.ktor.server.cio)
    val logbackVersion = "1.4.14"
    // Logback with Groovy support
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    // Database
    implementation("org.postgresql:postgresql:42.7.0")
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")

    // Redis
    implementation("redis.clients:jedis:5.0.2")
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Optional: Structured logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.koog.agents)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    configurations.all {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}

val profile = project.findProperty("profile") as String? ?: "dev"
when (profile) {
    "dev" -> tasks.withType<KotlinCompile> {
        println("profile: $profile")
        kotlinOptions {
            freeCompilerArgs
        }
    }

    else -> {}
}
