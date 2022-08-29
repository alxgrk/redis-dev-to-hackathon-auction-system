package de.alxgrk.authentication.oauth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.*

object RedirectUriStateEncoder {

    private const val QUERY_PARAMETER_KEY = "redirect_uri"

    private const val DELIMITER = ";"

    /**
     * Try to encode redirect_uri query parameter in state. If that parameter is not specified, do not override state.
     */
    fun URLBuilder.encodeRedirectUriInState(call: ApplicationCall) {
        val redirectUri = call.parameters[QUERY_PARAMETER_KEY]
        if (redirectUri != null)
            parameters["state"] = (parameters["state"].toString() + DELIMITER + redirectUri).encodeBase64()
    }

    /**
     * Try to retrieve the redirect_uri from state; otherwise, return null.
     */
    fun decodeRedirectUriFromState(call: ApplicationCall): String? {
        val state = call.request.queryParameters["state"]!!.decodeBase64String()
        return if (state.contains(";")) state.substringAfter(";") else null
    }
}
