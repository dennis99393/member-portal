package org.dallasmakerspace.testing.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.dallasmakerspace.testing.data.Car
import org.dallasmakerspace.testing.data.CarStorageMock

fun Application.configureRouting() {
  routing {
    route("cars") {
      get { call.respond(CarStorageMock.carStorage) }
      get("{id?}") {
        val id = call.parameters["id"]
        val car =
            CarStorageMock.carStorage.find { it.id == id }
                ?: return@get call.respondText(
                    text = "car.not.found",
                    status = HttpStatusCode.NotFound,
                )
        call.respond(car)
      }
      post {
        val car = call.receive<Car>()
        CarStorageMock.carStorage.add(car)
        call.respond(status = HttpStatusCode.Created, message = car)
      }
      put("{id?}") {
        val id = call.parameters["id"]
        val car =
            CarStorageMock.carStorage.find { it.id == id }
                ?: return@put call.respondText(
                    text = "car.not.found",
                    status = HttpStatusCode.NotFound,
                )
        val carUpdate = call.receive<Car>()
        car.brand = carUpdate.brand
        car.price = carUpdate.price
        call.respond(car)
      }
      delete("{id?}") {
        val id = call.parameters["id"]
        if (CarStorageMock.carStorage.removeIf { it.id == id }) {
          call.respondText(text = "car.deleted", status = HttpStatusCode.OK)
        } else {
          call.respondText(text = "car.not.found", status = HttpStatusCode.NotFound)
        }
      }
    }
  }
}
