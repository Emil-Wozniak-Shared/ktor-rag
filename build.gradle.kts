import io.ktor.plugin.features.DockerPortMapping
import io.ktor.plugin.features.DockerPortMappingProtocol
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotest)
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


    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")

    // Redis
//    implementation("redis.clients:jedis:5.0.2")
    implementation("io.github.domgew:kedis-jvm:0.0.9")

    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Use Groovy 4.x for both implementation and test
    implementation("org.apache.groovy:groovy-all:4.0.27")
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:2.0.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk-bdd:1.14.5")
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.koog.agents)
    implementation(libs.ktor.server.config.yaml)
    implementation(project(":logbackt"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation("com.h2database:h2:2.3.232")
}

val copyTestResources by tasks.registering(Copy::class) {
    from("src/test/resources")
    into("${buildDir}/resources/test")
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xallow-any-scripts-in-source-roots")
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    withType<Test>().configureEach {
        dependsOn(copyTestResources)
    }
    withType<io.kotest.framework.gradle.tasks.KotestJvmTask>().configureEach {
        dependsOn(copyTestResources)
    }
}

ktor {
    docker {
        localImageName.set("ejdev")
        portMappings.set(listOf(
            DockerPortMapping(
                outsideDocker = 8080,
                insideDocker = 8080,
                DockerPortMappingProtocol.TCP
            )
        ))
    }
}

