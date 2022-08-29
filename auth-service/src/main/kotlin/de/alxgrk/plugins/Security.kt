package de.alxgrk.plugins

import de.alxgrk.authentication.*
import de.alxgrk.authentication.oauth.*
import de.alxgrk.authentication.oauth.OAuthProvider.Companion.installOAuthProviders
import de.alxgrk.config.ConfigurationProperties
import io.ktor.server.application.*
import org.kodein.di.instance

fun Application.configureSecurity() {

    val props: ConfigurationProperties by di.instance()

    installOAuthProviders(
        GithubOAuthProvider(this),
    ) {
        adminAuth(props)
        jwtAuth(props)
    }

    jwtHandling()
}


