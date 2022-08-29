package de.alxgrk

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.client.request.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        val client = createClient {
            followRedirects = false
            expectSuccess = false
        }
        val response = client.get("/login-github")
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]!!.startsWith("https://github.com/"))
    }
}
