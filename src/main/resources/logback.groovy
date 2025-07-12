import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import static ch.qos.logback.classic.Level.*

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %highlight(%-5level) %cyan(%logger{36}) - %msg%n"
    }
}

root(INFO, ["CONSOLE"])

//import ch.qos.logback.classic.Level
//import ch.qos.logback.classic.encoder.PatternLayoutEncoder
////import ch.qos.logback.core.ConsoleAppender
//
//appender("STDOUT", ConsoleAppender) {
//    encoder(PatternLayoutEncoder) {
//        pattern = "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %X{call-id} %-5level %logger{36} - %msg%n"
//    }
//}
//root(Level.INFO, ["STDOUT"])
//logger(Level.INFO, ["org.eclipse.jetty"])
//logger(Level.INFO, ["io.netty"])
