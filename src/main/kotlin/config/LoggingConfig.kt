package pl.config

import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import pl.ejdev.logbackt.*

private const val CONSOLE = "console"
private const val PATTERN =
    "%magenta(%d{YYYY-MM-dd HH:mm:ss.SSS}) %highlight(%-15.15thread) %cyan(%X{call-id}) %green(%-5level) %logger{36} - %msg%n"

fun configureLogback() {
    context {
        appender(name = CONSOLE) {
            encoder<PatternLayoutEncoder> { pattern = PATTERN }
        }
        root(level = INFO)
        logger(level = INFO, "org.eclipse.jetty", "io.netty")
    }
}