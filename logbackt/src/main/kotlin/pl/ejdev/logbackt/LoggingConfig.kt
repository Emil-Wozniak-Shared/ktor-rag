package pl.ejdev.logbackt

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.Constructor
import javax.script.ScriptEngineManager

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
internal class LoggingConfig {
    init {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        val scriptPath = javaClass.getResource("/logback.kts").path
        val script = File(scriptPath).readText()

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            engine.eval(script)
        } finally {
            System.setOut(originalOut)
        }

        outputStream.toString()
    }
}

fun logbackt() {
    LoggingConfig()
}


@DslMarker
annotation class LoggingDsl

@LoggingDsl
fun context(
    config: LoggerContext.() -> Unit
): Unit = (LoggerFactory.getILoggerFactory() as LoggerContext).run {
    val root: Logger = getLogger("ROOT")
    root.detachAndStopAllAppenders()
    config()
    this.loggerList
}

@LoggingDsl
fun LoggerContext.root(level: Level) {
    val root: Logger = getLogger("ROOT")
    root.level = level
}


@LoggingDsl
fun LoggerContext.logger(
    level: Level, vararg names: String
) {
    names.forEach {
        val logger: Logger = getLogger(it)
        logger.level = level
    }
}

@LoggingDsl
fun LoggerContext.appender(
    name: String,
    configure: ConsoleAppender<ILoggingEvent?>.() -> Unit
) {
    val root: Logger = getLogger("ROOT")
    root.addAppender(ConsoleAppender<ILoggingEvent?>().apply {
        setContext(this@appender)
        setName(name)
        configure()
        start()
    })
}


@LoggingDsl
@Suppress("UNCHECKED_CAST")
inline fun <reified E> ConsoleAppender<ILoggingEvent?>.encoder(
    config: E.() -> Unit
) where E : Encoder<ILoggingEvent?> {
    val constructors = E::class.java.constructors
    val constructor: Constructor<E> = constructors.firstOrNull()!! as Constructor<E>
    this@encoder.setEncoder(constructor.newInstance().apply {
        context = this@encoder.context
        config(this)
        this.start()
    })
}
