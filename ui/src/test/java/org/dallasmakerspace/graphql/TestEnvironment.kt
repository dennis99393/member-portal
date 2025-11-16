package org.dallasmakerspace.graphql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun graphQlTestEnvironment(testFunction: suspend (HttpClient) -> Unit) {
  testApplication {
    application {}
    val client = createClient { install(ContentNegotiation) { json() } }
    testFunction(client)
  }
}

fun serializeQuery(query: String) = buildJsonObject { put("query", query.trimMargin()) }

suspend fun readResponseAsMap(response: HttpResponse): Map<*, *> =
    jacksonObjectMapper().readValue(response.bodyAsText(), Map::class.java)
