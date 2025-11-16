package org.dallasmakerspace.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel

val mockEngine = MockEngine { request ->
  when {
    request.headers["Authorization"] != "Basic YmFlbGR1bmc6YmFlbGR1bmc=" ->
        respond(
            content = "Wrong credentials!",
            status = HttpStatusCode.Unauthorized,
        )
    request.url.fullPath == "/cars" && request.method == HttpMethod.Get ->
        respond(
            content = ByteReadChannel(CARS),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    request.url.fullPath.startsWith("/driver") && request.method == HttpMethod.Get -> {
      val driverId = request.url.parameters["id"]?.toIntOrNull() ?: 0
      respond(
          content = ByteReadChannel(DRIVERS[driverId]),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    request.url.fullPath == "/car" && request.method == HttpMethod.Put ->
        respond(
            content = ByteReadChannel("Created!"),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    request.url.fullPath == "/driver" && request.method == HttpMethod.Put ->
        respond(
            content = ByteReadChannel("Created!"),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    else ->
        respond(
            content = ByteReadChannel("Unknown Request!"),
            status = HttpStatusCode.NotFound,
        )
  }
}
