package org.dallasmakerspace.server

import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlin.test.assertEquals
import org.dallasmakerspace.server.plugins.configureHttp
import org.dallasmakerspace.server.plugins.configureRouting
import org.dallasmakerspace.server.plugins.configureStatusPages
import org.dallasmakerspace.server.plugins.configureTemplating
import org.junit.AfterClass
import org.junit.BeforeClass
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

class ThymeleafServerIntegrationTest {

  companion object {

    private lateinit var server:
        EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    @BeforeClass
    @JvmStatic
    fun setup() {
      server =
          embeddedServer(Netty, host = "127.0.0.1", port = 8080) {
                configureHttp()
                configureRouting()
                configureTemplating()
                configureStatusPages()
              }
              .start(false)
    }

    @AfterClass
    @JvmStatic
    fun teardown() {
      server.stop(1000, 10000)
    }
  }

  // @Test
  fun `when get index then should return a list`() {
    val options = ChromeOptions()
    options.addArguments("--headless=new")
    val driver = ChromeDriver(options)
    driver.get("http://127.0.0.1:8080/")
    val listGroupItem = driver.findElements(By.className("list-group-item"))
    assertEquals(0, listGroupItem.size)
    driver.close()
  }

  // @Test
  fun `when get an invalid route then should return a default error page`() {
    val options = ChromeOptions()
    options.addArguments("--headless=new")
    val driver = ChromeDriver(options)
    driver.get("http://127.0.0.1:8080/other-page")
    val header2 = driver.findElements(By.tagName("h2"))
    assertEquals("Error - Not Found (404)", header2.first().text)
    driver.close()
  }
}
