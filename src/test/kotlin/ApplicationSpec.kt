package pl

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.config.yaml.*
import io.ktor.server.testing.*

class ApplicationSpec : FreeSpec({
    "Routes specification tests" - {
        testApplication {
            environment { config = yaml() }
            application { module(testing = true) }
            client.get("/") shouldHaveStatus OK
        }
    }
})

fun yaml(path: String = "application-test.yaml") =  YamlConfig(path)!!