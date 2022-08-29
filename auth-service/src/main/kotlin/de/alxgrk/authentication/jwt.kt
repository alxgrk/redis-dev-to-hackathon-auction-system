package de.alxgrk.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.alxgrk.config.ConfigurationProperties
import de.alxgrk.data.RefreshTokenRepository
import de.alxgrk.data.days
import de.alxgrk.plugins.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.kodein.di.instance
import java.util.*

val JWTPrincipal.userId
    get() = UUID.fromString(payload.getClaim("userId").asString())!!

@Serializable
data class TokenPair(val accessToken: String, val refreshToken: String)

@Serializable
data class RefreshToken(@Contextual val userId: UUID, @Contextual val value: UUID = UUID.randomUUID()) {
    fun encodeBase64() = "$userId;$value".encodeBase64()
}

private const val QUERY_PARAMETER_KEY = "redirect_uri"

fun String.decodeRefreshToken(): RefreshToken {
    val parts = this.decodeBase64String().split(';')
    if (parts.size != 2) throw IllegalArgumentException("Refresh token string not correctly formatted.")

    val (userId, value) = parts
    return RefreshToken(UUID.fromString(userId), UUID.fromString(value))
}

suspend fun ApplicationCall.generateTokenPair(userId: UUID): TokenPair {
    val props: ConfigurationProperties by application.di.instance()
    val refreshTokenRepository: RefreshTokenRepository by application.di.instance()

    val accessToken = JWT.create()
        .withAudience(props.jwt.audience)
        .withIssuer(props.jwt.issuer)
        .withClaim("userId", userId.toString())
        .withClaim("sourceIp", request.origin.remoteHost)
        .withExpiresAt(Date(System.currentTimeMillis() + 30 * 60000)) // 30 minutes
        .sign(Algorithm.HMAC256(props.jwt.secret))
    val refreshToken = RefreshToken(userId)

    refreshTokenRepository.update(userId, refreshToken, 7.days)

    return TokenPair(accessToken, refreshToken.encodeBase64())
}

fun Application.jwtHandling() {
    val refreshTokenRepository: RefreshTokenRepository by di.instance()

    routing {
        get("/token/refresh") {
            val oldRefreshTokenString = call.request.queryParameters["token"]
                ?: call.sessions.get<RefreshTokenCookie>()?.content
                ?: throw IllegalArgumentException("No refresh token specified in request.")
            val oldRefreshToken = oldRefreshTokenString.decodeRefreshToken()
            val userId = oldRefreshToken.userId
            val expiryDate = refreshTokenRepository.getExpiryDateIfExists(oldRefreshToken)

            if (expiryDate == null) {
                refreshTokenRepository.clearAllForUser(userId)
                throw AuthenticationException("Invalid refresh token submitted.")
            }
            if (expiryDate.millis <= System.currentTimeMillis()) {
                throw AuthenticationException("Refresh token expired.")
            }

            refreshTokenRepository.invalidate(oldRefreshToken)
            val tokenPair = call.generateTokenPair(userId)

            if (call.sessions.get<RefreshTokenCookie>()?.content != null) {
                // set token pair as separate cookie in order to make usage in web-frontend easier
                call.sessions.set(AccessTokenCookie(tokenPair.accessToken))
                call.sessions.set(RefreshTokenCookie(tokenPair.refreshToken))
                call.respond(HttpStatusCode.OK)
            } else
                call.respond(tokenPair)
        }

        authenticate(AllowedAuthenticationTypes.JWTAuth.key) {
            get("/logout") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respondRedirect("/")
                    return@get
                }

                call.sessions.clear<AccessTokenCookie>()
                call.sessions.clear<RefreshTokenCookie>()

                val userId = principal.userId
                refreshTokenRepository.clearAllForUser(userId)

                call.respondRedirect(call.request.queryParameters[QUERY_PARAMETER_KEY] ?: "/")
            }
        }
    }
}

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
