package org.dallasmakerspace.discourse

import java.io.IOException

class DiscourseApiException(message: String, cause: Exception? = null) :
    IOException(message, cause)
