package de.alxgrk.authentication.oauth

import de.alxgrk.authentication.AllowedAuthenticationTypes
import de.alxgrk.authentication.generateTokenPair
import de.alxgrk.common.User
import de.alxgrk.data.UserRepository
import de.alxgrk.plugins.AccessTokenCookie
import de.alxgrk.plugins.AuthenticationException
import de.alxgrk.plugins.RefreshTokenCookie
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json

interface OAuthUser {
    val externalId: String
    fun mapToUser(): User
}

interface OAuthProvider {
    val application: Application
    val name: String
    fun OAuthAuthenticationProvider.Config.providerConfig()
    suspend fun OAuthAccessTokenResponse.OAuth2.toUser(): User

    companion object {
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        suspend inline fun <reified User : OAuthUser> OAuthAccessTokenResponse.OAuth2.toUserByRequesting(
            userRepository: UserRepository,
            url: String,
            accept: String = "application/json"
        ): de.alxgrk.common.User {
            val response =
                httpClient.get(url) {
                    headers {
                        this["Authorization"] = "Bearer ${this@toUserByRequesting.accessToken}"
                        this["Accept"] = accept
                    }
                }
            val user: User = response.body()
            val foundUser = userRepository.findByExternalId(user.externalId)
            return foundUser ?: run {
                user.mapToUser()
                    .also { userRepository.insert(it) }
            }
        }

        fun Application.installOAuthProviders(
            vararg providers: OAuthProvider,
            additionalAuthConfig: AuthenticationConfig.() -> Unit
        ) {
            authentication {
                providers.forEach { provider ->
                    val type = AllowedAuthenticationTypes.OAuth(provider)
                    oauth(type.key) {
                        with(provider) {
                            providerConfig()
                            client = httpClient
                        }
                    }
                }
                additionalAuthConfig()
            }
            routing {
                providers.forEach { provider ->
                    val name = provider.name
                    val type = AllowedAuthenticationTypes.OAuth(provider)
                    authenticate(type.key) {
                        get("/login-$name") {
                            // Redirects to 'authorizeUrl' automatically
                        }

                        get("/callback-$name") {
                            val principal: OAuthAccessTokenResponse.OAuth2 = call.authentication.principal()
                                ?: throw AuthenticationException("No principal found on $name callback.")

                            val user = with(provider) { principal.toUser() }

                            val tokenPair = call.generateTokenPair(user.id)

                            val redirectUri = RedirectUriStateEncoder.decodeRedirectUriFromState(call)
                            if (redirectUri != null) {
                                // set token pair as separate cookie in order to make usage in web-frontend easier
                                call.sessions.set(AccessTokenCookie(tokenPair.accessToken))
                                call.sessions.set(RefreshTokenCookie(tokenPair.refreshToken))
                                call.respondRedirect(redirectUri)
                            } else
                                call.respond(tokenPair)
                        }
                    }
                }
            }
        }
    }
}
