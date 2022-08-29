package de.alxgrk

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.config.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        val response = client.post("/auctions/${UUID.randomUUID()}/bidding") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(mapOf("amount" to "123.45", "currency" to "EUR")))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
