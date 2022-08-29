package de.alxgrk.authentication.oauth

import de.alxgrk.authentication.oauth.OAuthProvider.Companion.toUserByRequesting
import de.alxgrk.authentication.oauth.RedirectUriStateEncoder.encodeRedirectUriInState
import de.alxgrk.common.AuthType
import de.alxgrk.common.User
import de.alxgrk.config.ConfigurationProperties
import de.alxgrk.data.UserDocument
import de.alxgrk.data.UserRepository
import de.alxgrk.plugins.di
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable
import org.kodein.di.instance

class GithubOAuthProvider(override val application: Application) : OAuthProvider {
    override val name: String = "github"

    private val props: ConfigurationProperties by application.di.instance()
    private val userRepository: UserRepository by application.di.instance()

    override fun OAuthAuthenticationProvider.Config.providerConfig() {
        urlProvider = { "http://localhost:8080/callback-github" }
        providerLookup = {
            val call = this
            OAuthServerSettings.OAuth2ServerSettings(
                name = this@GithubOAuthProvider.name,
                authorizeUrl = "https://github.com/login/oauth/authorize",
                accessTokenUrl = "https://github.com/login/oauth/access_token",
                requestMethod = HttpMethod.Post,
                clientId = props.githubCredentials.clientId,
                clientSecret = props.githubCredentials.clientSecret,
                defaultScopes = listOf("read:user", "user:email"),
                authorizeUrlInterceptor = {
                    with(RedirectUriStateEncoder) { encodeRedirectUriInState(call) }
                }
            )
        }
    }

    override suspend fun OAuthAccessTokenResponse.OAuth2.toUser(): User =
        toUserByRequesting<GithubUser>(
            userRepository,
            "https://api.github.com/user",
            "application/vnd.github.v3+json"
        )
}

@Serializable
data class GithubUser(
    val id: Long,
    val login: String,
    val name: String,
    val email: String,
    val company: String? = null,
    val html_url: String? = null,
    val avatar_url: String?
): OAuthUser {

    override val externalId = id.toString()

    override fun mapToUser(): User {
        return UserDocument(
            externalId = externalId,
            login = login,
            name = name,
            email = email,
            authType = AuthType.Github,
            organization = company,
            homepageUrl = html_url,
            avatarUrl = avatar_url,
            address = null,
            paymentInfo = null
        )
    }
}
