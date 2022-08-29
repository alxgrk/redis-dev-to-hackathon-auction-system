package de.alxgrk.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.alxgrk.config.ConfigurationProperties
import de.alxgrk.plugins.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.sessions.*
import java.util.*

val JWTPrincipal.userId
    get() = UUID.fromString(payload.getClaim("userId").asString())!!

fun AuthenticationConfig.jwtAuth(props: ConfigurationProperties) {
    jwt(AllowedAuthenticationTypes.JWTAuth.key) {
        realm = props.jwt.realm
        authHeader { call ->
            val authHeaderOrNull = try {
                call.request.parseAuthorizationHeader()
            } catch (ex: IllegalArgumentException) {
                null
            }
            authHeaderOrNull
                ?: parseAuthorizationHeader("Bearer ${call.sessions.get<AccessTokenCookie>()?.content}")
        }
        verifier(
            JWT.require(Algorithm.HMAC256(props.jwt.secret))
                .withAudience(props.jwt.audience)
                .withIssuer(props.jwt.issuer)
                .build()
        )
        validate { credential ->
            if (credential.payload.audience.contains(props.jwt.audience)
                && (credential.payload.expiresAt?.time ?: 0) > System.currentTimeMillis()
                && credential.payload.getClaim("userId").asString() != ""
                && credential.payload.getClaim("sourceIp").asString() == request.origin.remoteHost
            )
                JWTPrincipal(credential.payload)
            else null
        }
    }
}
