package pl

import io.ktor.server.application.*
import io.ktor.server.cio.*
import pl.config.configureLogback

/**
* ### For dev config
* `-config=application-dev.yaml`
 * */
fun main(args: Array<String>) {
    configureLogback()
    EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureMonitoring()
    configureSecurity()
    configureRouting()
}

