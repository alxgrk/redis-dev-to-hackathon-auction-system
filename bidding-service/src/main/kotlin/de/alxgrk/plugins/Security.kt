package de.alxgrk.plugins

import de.alxgrk.authentication.*
import de.alxgrk.config.ConfigurationProperties
import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.kodein.di.instance

fun Application.configureSecurity() {

    val props: ConfigurationProperties by di.instance()

    authentication {
        adminAuth(props)
        jwtAuth(props)
    }
}


