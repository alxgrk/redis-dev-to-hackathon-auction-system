package de.alxgrk.plugins

import de.alxgrk.config.ConfigurationProperties
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import org.kodein.di.instance

const val ACCESS_TOKEN_COOKIE_NAME = "AUCTION_SYSTEM_ACCESS_TOKEN"
const val REFRESH_TOKEN_COOKIE_NAME = "AUCTION_SYSTEM_REFRESH_TOKEN"

@JvmInline
value class AccessTokenCookie(val content: String)
@JvmInline
value class RefreshTokenCookie(val content: String)

fun Application.configureSessions() {

    val props by di.instance<ConfigurationProperties>()
    val (encryptionKey, signingKey) = props.cookies
    val secureCookies = !props.developmentMode

    install(Sessions) {
        cookie<AccessTokenCookie>(ACCESS_TOKEN_COOKIE_NAME) {

            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            cookie.secure = secureCookies
            cookie.path = "/"

            transform(SessionTransportTransformerEncrypt(encryptionKey, signingKey))
        }
        cookie<RefreshTokenCookie>(REFRESH_TOKEN_COOKIE_NAME) {

            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            cookie.secure = secureCookies
            cookie.path = "/token/refresh"

            transform(SessionTransportTransformerEncrypt(encryptionKey, signingKey))
        }
    }

}
