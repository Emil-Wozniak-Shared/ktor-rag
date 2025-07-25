package pl.ext

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString

private const val ENVIRONMENT = "ktor.environment"
private const val TEST = "TEST"

val ApplicationConfig.test: Boolean
    get() = this.tryGetString(ENVIRONMENT) == TEST

val ApplicationConfig.notTest: Boolean
    get() = this.tryGetString(ENVIRONMENT) != TEST