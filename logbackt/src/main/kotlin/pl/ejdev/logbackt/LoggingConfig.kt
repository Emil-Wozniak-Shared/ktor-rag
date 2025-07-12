package pl.ejdev.logbackt

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import org.slf4j.LoggerFactory
import java.lang.reflect.Constructor

@DslMarker
annotation class LoggingDsl

@LoggingDsl
fun context(
    rootLevel: Level,
    config: LoggerContext.(root: Logger) -> Unit
) = (LoggerFactory.getILoggerFactory() as LoggerContext).apply {
    val root: Logger = getLogger("ROOT")
    root.detachAndStopAllAppenders()
    config(root)
    root.setLevel(rootLevel)
}


@LoggingDsl
fun LoggerContext.appender(
    name: String,
    root: Logger,
    configure: ConsoleAppender<ILoggingEvent?>.(ctx: LoggerContext) -> Unit
) {
    root.addAppender(ConsoleAppender<ILoggingEvent?>().apply {
        setContext(this@appender)
        setName(name)
        configure(this@appender)
        start()
    })
}


@LoggingDsl
@Suppress("UNCHECKED_CAST")
inline fun <reified E> ConsoleAppender<ILoggingEvent?>.encoder(
    ctx: LoggerContext,
    config: E.() -> Unit
) where E: Encoder<ILoggingEvent?> {
    val constructors = E::class.java.constructors
    val constructor: Constructor<E> = constructors.firstOrNull()!! as Constructor<E>
    this@encoder.setEncoder(constructor.newInstance().apply {
        context = ctx
        config(this)
        this.start()
    })
}
