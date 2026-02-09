package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory

class ManifestHandler
@Inject
constructor(loggerFactory: LoggerFactory, private val appConfig: AppConfig) : IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  @Serializable
  data class ManifestIcon(
      val src: String,
      val sizes: String,
      val type: String,
      val purpose: String? = null
  )

  @Serializable
  data class WebAppManifest(
      val name: String,
      val short_name: String,
      val description: String,
      val start_url: String,
      val scope: String,
      val display: String,
      val orientation: String,
      val theme_color: String,
      val background_color: String,
      val categories: List<String>,
      val icons: List<ManifestIcon>
  )

  override suspend fun handle(call: ApplicationCall) {
    log.debug("ManifestHandler generating manifest.json")

    val baseUrl = appConfig.requireStringProperty("app.base-url")

    val manifest =
        WebAppManifest(
            name = "Dallas Makerspace Member Portal",
            short_name = "DMS Portal",
            description = "Dallas Makerspace member profiles, groups, reports, and more",
            start_url = "$baseUrl/",
            scope = "$baseUrl/",
            display = "standalone",
            orientation = "portrait-primary",
            theme_color = "#0b9cc9",
            background_color = "#ffffff",
            categories = listOf("productivity", "utilities"),
            icons =
                listOf(
                    ManifestIcon("$baseUrl/static/icons/icon-72x72.png", "72x72", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-96x96.png", "96x96", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-128x128.png", "128x128", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-144x144.png", "144x144", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-152x152.png", "152x152", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-192x192.png", "192x192", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-384x384.png", "384x384", "image/png"),
                    ManifestIcon("$baseUrl/static/icons/icon-512x512.png", "512x512", "image/png"),
                    ManifestIcon(
                        "$baseUrl/static/icons/icon-192x192-maskable.png",
                        "192x192",
                        "image/png",
                        "maskable"),
                    ManifestIcon(
                        "$baseUrl/static/icons/icon-512x512-maskable.png",
                        "512x512",
                        "image/png",
                        "maskable")))

    val json = Json { prettyPrint = true }
    call.respondText(
        json.encodeToString(manifest),
        ContentType.parse("application/manifest+json"),
        HttpStatusCode.OK)
  }
}
