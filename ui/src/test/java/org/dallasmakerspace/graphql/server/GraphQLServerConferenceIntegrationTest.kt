package org.dallasmakerspace.graphql.server

private const val CREATE_CONFERENCE_QUERY =
    """
    mutation {
        saveOrCreateConference(conference: { attendees: [ ], name: "My Conference" }) {
            id
            name
            attendees
      }
    }
    """

private const val UPDATE_CONFERENCE_QUERY =
    """
    mutation {
        saveOrCreateConference(conference: { id: 0, attendees: [ ], name: "My Conference" }) {
            id
            name
            attendees
      }
    }
    """

private const val FIND_BY_ID_QUERY =
    """
    {
        conferenceById(id: 0) {
            name
            id
        }
    }
    """
