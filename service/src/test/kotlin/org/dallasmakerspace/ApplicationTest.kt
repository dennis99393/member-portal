package org.dallasmakerspace

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.dallasmakerspace.plugins.configureRouting

class ApplicationTest {
  @Test
  fun testRoot() = testApplication {
    application { configureRouting() }
    client.get("/").apply { assertEquals(HttpStatusCode.OK, status) }
  }
}
