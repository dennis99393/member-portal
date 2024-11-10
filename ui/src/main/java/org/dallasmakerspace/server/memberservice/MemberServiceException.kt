package org.dallasmakerspace.server.memberservice

class MemberServiceException(message: String, cause: Throwable?) : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}
