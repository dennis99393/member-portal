package org.dallasmakerspace.graphql.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import junit.framework.TestCase.assertEquals
import kotlin.test.*

private const val CREATE_CONFERENCE_QUERY = """
    mutation {
        saveOrCreateConference(conference: { attendees: [ ], name: "My Conference" }) {
            id
            name
            attendees
      }
    }
    """

private const val UPDATE_CONFERENCE_QUERY = """
    mutation {
        saveOrCreateConference(conference: { id: 0, attendees: [ ], name: "My Conference" }) {
            id
            name
            attendees
      }
    }
    """

private const val FIND_BY_ID_QUERY = """
    {
        conferenceById(id: 0) {
            name
            id
        }
    }
    """

