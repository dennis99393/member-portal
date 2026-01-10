package org.dallasmakerspace.server.linkedin

/**
 * Data class representing the state stored during LinkedIn OAuth flow. Contains both CSRF
 * protection state and the user-provided LinkedIn username.
 */
data class LinkedInOAuthState(val csrfState: String, val linkedinUsername: String)
