package org.dallasmakerspace.server.common.logging

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import java.security.cert.X509Certificate
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
