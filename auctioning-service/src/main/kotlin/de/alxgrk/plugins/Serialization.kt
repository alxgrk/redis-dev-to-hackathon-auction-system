package de.alxgrk.plugins

import de.alxgrk.common.UUIDSerializer
import de.alxgrk.common.UrlSerializer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.util.*

val JsonSerializer = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        contextual(UUIDSerializer)
        contextual(UrlSerializer)
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(JsonSerializer)
    }
}
