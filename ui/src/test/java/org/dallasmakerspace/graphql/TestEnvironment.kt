package org.dallasmakerspace.graphql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val OBJECT_BY_ID_TEST_QUERY =
    """
    {
        objectById(id: 0) {
            id
        }
    }
    """

fun graphQlTestEnvironment(testFunction: suspend (HttpClient) -> Unit) {
  testApplication {
    application {}
    val client = createClient {
      install(ContentNegotiation) { json() }
      install(WebSockets)
    }
    testFunction(client)
  }
}

fun serializeQuery(query: String) = buildJsonObject { put("query", query.trimMargin()) }

suspend fun readResponseAsMap(response: HttpResponse): Map<*, *> =
    jacksonObjectMapper().readValue(response.bodyAsText(), Map::class.java)
