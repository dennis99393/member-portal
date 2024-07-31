package org.dallasmakerspace.thymeleaf.server.common.logging

import io.ktor.util.logging.*

public fun appLogger(simpleName: String) = KtorSimpleLogger(simpleName)
