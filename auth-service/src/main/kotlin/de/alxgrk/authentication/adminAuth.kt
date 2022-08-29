package de.alxgrk.authentication

import de.alxgrk.config.ConfigurationProperties
import io.ktor.server.auth.*
import io.ktor.util.*

private val digestFunction = getDigestFunction("SHA-256") { "ktor${it.length}" }

fun AuthenticationConfig.adminAuth(
    props: ConfigurationProperties
) {

    val hashedUserTable = UserHashedTableAuth(
        table = mapOf(props.adminAuth.username to digestFunction(props.adminAuth.password)),
        digester = digestFunction
    )

    basic(AllowedAuthenticationTypes.AdminAuth.key) {
        realm = props.adminAuth.realm
        validate { credentials ->
            hashedUserTable.authenticate(credentials)
        }
    }
}
