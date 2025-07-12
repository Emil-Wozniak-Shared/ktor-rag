package pl

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {
    routing {
        route("/callback") {
            install(DoubleReceive)
//            install(LineWebhook) {
//                channelSecret = System.getenv("CHANNEL_SECRET")
//            }
            post {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
