package org.dallasmakerspace.server.common.logging

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import org.apache.http.HttpHost
import org.apache.http.HttpRequestInterceptor
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.ssl.SSLContextBuilder
import org.dallasmakerspace.server.di.DaggerAppComponent
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder

object ElasticsearchClientManager {
  private const val FAILURE_THRESHOLD = 3
  private const val COOLDOWN_MS = 60_000L

  private val consecutiveFailures = AtomicInteger(0)
  private val circuitOpenedAt = AtomicLong(0)

  /**
   * Returns true if the circuit is closed (ES calls should proceed). Returns false when the circuit
   * is open due to repeated failures, allowing the cooldown period to elapse before retrying.
   */
  fun isAvailable(): Boolean {
    val failures = consecutiveFailures.get()
    if (failures < FAILURE_THRESHOLD) return true
    return System.currentTimeMillis() - circuitOpenedAt.get() > COOLDOWN_MS
  }

  fun recordSuccess() {
    val prev = consecutiveFailures.getAndSet(0)
    if (prev >= FAILURE_THRESHOLD) {
      log.info("Elasticsearch connection recovered after $prev consecutive failures")
    }
  }

  fun recordFailure() {
    val failures = consecutiveFailures.incrementAndGet()
    if (failures == FAILURE_THRESHOLD) {
      circuitOpenedAt.set(System.currentTimeMillis())
      log.warn(
          "Elasticsearch circuit breaker opened after $failures consecutive failures, " +
              "pausing for ${COOLDOWN_MS}ms")
    }
  }

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
        RestClient.builder(HttpHost(esHost, 9200, "https"))
            .setHttpClientConfigCallback { httpClientBuilder }
            .setRequestConfigCallback { requestConfigBuilder ->
              requestConfigBuilder
                  .setConnectTimeout(3000)
                  .setSocketTimeout(10_000)
                  .setConnectionRequestTimeout(3000)
            }

    val restClient = restClientBuilder.build()
    val transport = RestClientTransport(restClient, JacksonJsonpMapper())
    ElasticsearchClient(transport)
  }
}
