package org.dallasmakerspace.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class RequestsUnitTest {

  companion object {

    private val client = HttpClient(mockEngine) {
      expectSuccess = true
      install(ContentNegotiation) {
        jackson()
      }
      install(Auth) {
        basic {
          credentials {
            BasicAuthCredentials(username = "baeldung", password = "baeldung")
          }
          sendWithoutRequest { _ -> true }
        }
      }
    }


    private val noAuthClient = HttpClient(mockEngine) {
      install(ContentNegotiation) {
        jackson()
      }
    }

    @JvmStatic
    @AfterClass
    fun afterTests() {
      client.close()
    }
  }

  @Test
  fun `when fetching cars then should get two cars`() {
    runBlocking {
      with(client.get("/cars")) {
        assertEquals(HttpStatusCode.OK, status)
        val cars: List<Car> = body()
        assertEquals(2, cars.size)
        assertTrue { cars.any { car -> car.name == "Car 1" } }
      }
    }
  }

  @Test
  fun `when fetching driver zero then should get driver`() {
    runBlocking {
      with(client.get("/driver?id=0")) {
        assertEquals(HttpStatusCode.OK, status)
        val driver: Driver = body()
        assertEquals(0, driver.id)
      }
    }
  }

  @Test
  fun `when creating car then should succeed`() {
    runBlocking {
      with(
          client.put("/car") {
            contentType(ContentType.Application.Json)
            setBody(Car(id = 2, name = "Car 3", driver = 1))
          },
      ) {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals("Created!", bodyAsText())
      }
    }
  }

  @Test
  fun `when requesting unknown endpoint then should throw exception`() {
    runBlocking {
      try {
        client.get("/this-does-not-exist")
      } catch (exception: ClientRequestException) {
        return@runBlocking
      }
      fail("Did not throw an exception!")
    }
  }

  @Test
  fun `when creating driver then should succeed`() {
    runBlocking {
      with(
          client.put("/driver") {
            contentType(ContentType.Application.Json)
            setBody(Driver(id = 2, name = "Jack"))
          },
      ) {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals("Created!", bodyAsText())
      }
    }
  }


  @Test
  fun `when not sending authentication then should not succeed`() {
    runBlocking {
      with(noAuthClient.get("/cars")) {
        assertEquals(HttpStatusCode.Unauthorized, status)
      }
    }
  }

  private fun startEmbeddedServer(): NettyApplicationEngine {
    val env = applicationEngineEnvironment {
      module {
      }
      connector {
        host = "0.0.0.0"
        port = 8080
      }
    }
    val server = embeddedServer(Netty, env)
    server.start(false)
    return server
  }

}
