package org.dallasmakerspace.thymeleaf.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.dallasmakerspace.thymeleaf.server.plugins.configureRouting
import org.dallasmakerspace.thymeleaf.server.plugins.configureStatusPages
import org.dallasmakerspace.thymeleaf.server.plugins.configureTemplating
import org.junit.AfterClass
import org.junit.BeforeClass
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

class ThymeleafServerIntegrationTest {

  companion object {

    private lateinit var server: NettyApplicationEngine

    @BeforeClass
    @JvmStatic
    fun setup() {
      val env = applicationEngineEnvironment {
        module {
          configureRouting()
          configureTemplating()
          configureStatusPages()
        }
        connector {
          host = "127.0.0.1"
          port = 8080
        }
      }
      server = embeddedServer(Netty, env).start(false)
    }

    @AfterClass
    @JvmStatic
    fun teardown() {
      server.stop(1000, 10000)
    }
  }

  @Test
  fun `when get index then should return a list`() {
    val options = ChromeOptions()
    options.addArguments("--headless=new")
    val driver = ChromeDriver(options)
    driver.get("http://127.0.0.1:8080/")
    val listGroupItem = driver.findElements(By.className("list-group-item"))
    assertEquals(0, listGroupItem.size)
    driver.close()
  }

  @Test
  fun `when get report-card then should return a form and table`() {
    val options = ChromeOptions()
    options.addArguments("--headless=new")
    val driver = ChromeDriver(options)
    driver.get("http://127.0.0.1:8080/report-card/1")
    val form = driver.findElement(By.tagName("form"))
    assertNotNull(form)
    val readingInput = driver.findElement(By.name("1"))
    readingInput.sendKeys("A")
    val writingInput = driver.findElement(By.name("2"))
    writingInput.sendKeys("A+")
    val scienceInput = driver.findElement(By.name("3"))
    scienceInput.sendKeys("B")
    val mathematicsInput = driver.findElement(By.name("4"))
    mathematicsInput.sendKeys("B+")
    val submit = driver.findElement(By.className("btn-primary"))
    submit.submit()

    driver.close()
  }

  @Test
  fun `when get an invalid route then should return a default error page`() {
    val options = ChromeOptions()
    options.addArguments("--headless=new")
    val driver = ChromeDriver(options)
    driver.get("http://127.0.0.1:8080/other-page")
    val header2 = driver.findElements(By.tagName("h2"))
    assertEquals("Error", header2.first().text)
    driver.close()
  }
}
