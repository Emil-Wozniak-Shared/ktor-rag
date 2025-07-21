package pl

import com.sksamuel.cohort.Cohort
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.cpu.ProcessCpuHealthCheck
import com.sksamuel.cohort.memory.FreememHealthCheck
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import kotlinx.coroutines.Dispatchers
import pl.ext.test
import kotlin.time.Duration.Companion.seconds

fun Application.configureMonitoring() {
    val test = this.environment.config.test
    if (!test) {
        install(Cohort) {
            operatingSystem = true
            jvmInfo = true
            sysprops = true
            heapDump = true
            threadDump = true
            verboseHealthCheckResponse = true
            healthcheck("/health", HealthCheckRegistry(Dispatchers.Default) {
                register(FreememHealthCheck.mb(250), 10.seconds, 10.seconds)
                register(ProcessCpuHealthCheck(0.8), 10.seconds, 10.seconds)
            })
        }
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify(String::isNotEmpty)
    }
    install(CallLogging) {
        callIdMdc("call-id")
    }
}
