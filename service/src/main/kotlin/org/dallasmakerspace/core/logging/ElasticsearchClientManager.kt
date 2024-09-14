package org.dallasmakerspace.core.logging

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import java.io.IOException
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpHost
import org.apache.http.HttpRequestInterceptor
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.ssl.SSLContextBuilder
import org.dallasmakerspace.di.DaggerAppComponent
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.slf4j.MDC

object ElasticsearchClientManager {
  // We can't use Dagger to inject dependencies since this class is called from a library
  private val appConfig = DaggerAppComponent.create().getAppConfig()
  private val esApiKey = appConfig.requireStringProperty("app.logging.elastic-api-key")
  private val esHost = appConfig.requireStringProperty("app.logging.elastic-host")
  private val loggerFactory = DaggerAppComponent.create().getLoggerFactory()
  val log = loggerFactory.create(ElasticsearchClientManager::class.java)
  val client: ElasticsearchClient by lazy {
    // Create an SSLContext that ignores self-signed certificates
    // TODO: verify the server's certificate
    val sslContext: SSLContext =
        SSLContextBuilder.create()
            .loadTrustMaterial { _: Array<X509Certificate>, _: String -> true }
            .build()

    // Create a HostnameVerifier
    val hostnameVerifier = HostnameVerifier { hostname: String, _: SSLSession ->
      hostname == esHost
    }

    val httpClientBuilder: HttpAsyncClientBuilder =
        HttpAsyncClients.custom()
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(hostnameVerifier)
            .addInterceptorLast(
                HttpRequestInterceptor { request, _ ->
                  request.addHeader("Authorization", "ApiKey $esApiKey")
                })

    @Suppress("MagicNumber")
    val restClientBuilder: RestClientBuilder =
        RestClient.builder(HttpHost(esHost, 9200, "https")).setHttpClientConfigCallback {
          httpClientBuilder
        }

    val restClient = restClientBuilder.build()
    val transport = RestClientTransport(restClient, JacksonJsonpMapper())
    ElasticsearchClient(transport)
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.logToElasticsearch(client: ElasticsearchClient) {
  val request = call.request
  val response = call.response
  // Do not log redirect responses and static files
  if (response.status() == HttpStatusCode.TemporaryRedirect ||
      request.uri.startsWith("/static") ||
      request.uri.startsWith("/favicon.ico")) {
    return
  }

  val logEntry =
      mapOf(
          "@timestamp" to Instant.now().toString(),
          "method" to request.httpMethod.value,
          "app" to "member-profile-ui",
          "host" to request.host(),
          "ip" to request.origin.remoteAddress,
          "sessionid" to MDC.get("CallId")?.toString(),
          "userid" to MDC.get("userid")?.toString(),
          "uri" to request.uri,
          "status" to response.status()?.value,
          "userAgent" to request.userAgent())

  withContext(Dispatchers.IO) {
    try {
      client.index { i -> i.index("logs").document(logEntry) }
    } catch (e: IOException) {
      ElasticsearchClientManager.log.error("Failed to log to Elasticsearch", e)
    }
  }
}
