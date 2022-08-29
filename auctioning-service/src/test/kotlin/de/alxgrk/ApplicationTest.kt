package de.alxgrk

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.config.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World!", response.bodyAsText())
    }
}
