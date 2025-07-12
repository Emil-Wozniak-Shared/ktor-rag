package pl

import io.ktor.server.application.*
import io.ktor.server.cio.*

/**
* ### For dev config
* `-config=application-dev.yaml`
 * */
fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureFrameworks()
    configureMonitoring()
    configureSecurity()
    configureRouting()
}

