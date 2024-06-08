package org.dallasmakerspace

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.dallasmakerspace.plugins.configureRouting
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  @Test
  fun testRoot() = testApplication {
    application { configureRouting() }
    client.get("/").apply { assertEquals(HttpStatusCode.OK, status) }
  }
}
