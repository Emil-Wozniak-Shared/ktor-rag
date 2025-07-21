package pl

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel

object KotestProjectConfig : AbstractProjectConfig() {
    override val parallelism = 1
    override val logLevel: LogLevel = LogLevel.Trace
}