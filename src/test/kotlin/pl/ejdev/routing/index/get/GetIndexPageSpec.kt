package pl.pl.ejdev.routing.index.get

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.OK
import pl.pl.ejdev.routing.ApiSpec
import kotlin.test.Test

class GetIndexPageSpec : ApiSpec() {
    @Test
    fun `GET static resource works`() = appSpec {
        // expect
        client.get("/") shouldHaveStatus OK
    }
}