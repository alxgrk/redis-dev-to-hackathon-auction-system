package de.alxgrk.plugins

import de.alxgrk.config.loadConfigurationProperties
import de.alxgrk.data.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.kodein.di.*
import org.kodein.di.ktor.KodeinDIKey

fun Application.configureDiAsync() {

    val diInstance = DI {
        val configurationProperties = loadConfigurationProperties()
        bindInstance { configurationProperties }

        dataModule(configurationProperties)
    }

    attributes.put(KodeinDIKey, diInstance)
}

val Application.di: DI
    get() = attributes[KodeinDIKey]

