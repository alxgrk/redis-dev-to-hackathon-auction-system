package de.alxgrk.plugins

import de.alxgrk.authentication.AllowedAuthenticationTypes
import io.ktor.server.plugins.*
import io.micrometer.prometheus.*
import io.ktor.server.metrics.micrometer.*
import org.slf4j.event.*
import io.ktor.server.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureMonitoring() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    routing {
        authenticate(AllowedAuthenticationTypes.AdminAuth.key) {
            get("/metrics-micrometer") {
                call.respond(appMicrometerRegistry.scrape())
            }
        }
    }
}
