package org.dallasmakerspace.server.models

import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember

data class SearchPreloadResponse(val members: List<DMSMember>, val groups: List<DMSGroup>)
