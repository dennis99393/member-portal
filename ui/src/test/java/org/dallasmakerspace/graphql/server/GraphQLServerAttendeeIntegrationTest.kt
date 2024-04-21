package org.dallasmakerspace.graphql.server

private const val CREATE_ATTENDEE_QUERY = """
    mutation {
        saveOrCreateAttendee(attendee: { name: "John Johnson" }) {
            id
            name
      }
    }
    """

private const val UPDATE_ATTENDEE_QUERY = """
    mutation {
        saveOrCreateAttendee(attendee: { id: 0, name: "Jake Jakeson" }) {
            id
            name
      }
    }
    """

