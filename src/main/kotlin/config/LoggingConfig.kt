package pl.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import pl.ejdev.logbackt.appender
import pl.ejdev.logbackt.context
import pl.ejdev.logbackt.encoder

fun configureLogback() {
    context(rootLevel = Level.INFO) { root ->
        appender("console", root) { ctx ->
            encoder<PatternLayoutEncoder>(ctx) {
                pattern =
                    "%magenta(%d{YYYY-MM-dd HH:mm:ss.SSS}) %highlight(%-15.15thread) %cyan(%X{call-id}) %green(%-5level) %logger{36} - %msg%n"
            }
        }
    }
}