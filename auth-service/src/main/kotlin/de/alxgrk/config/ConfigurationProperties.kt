package de.alxgrk.config

import com.okta.jwt.JwtVerifiers
import io.ktor.server.application.*
import io.ktor.util.*

fun Application.loadConfigurationProperties(): ConfigurationProperties = ConfigurationProperties(
    developmentMode = stringFromEnv("ktor.development").toBoolean(),

    jwt = ConfigurationProperties.JWT(
        secret = stringFromEnv("jwt.secret"),
        audience = stringFromEnv("jwt.audience"),
        realm = stringFromEnv("jwt.realm"),
        issuer = stringFromEnv("jwt.issuer")
    ),

    adminAuth = ConfigurationProperties.AdminAuth(
        realm = stringFromEnv("admin.realm"),
        username = stringFromEnv("admin.username"),
        password = stringFromEnv("admin.password")
    ),

    cookies = ConfigurationProperties.Cookies(
        encryptionKey = hex(stringFromEnv("cookies.encryptionKey")),
        signingKey = hex(stringFromEnv("cookies.signingKey"))
    ),

    db = ConfigurationProperties.DB(stringFromEnv("db.redis")),

    githubCredentials = ConfigurationProperties.OAuthCredentials(
        stringFromEnv("github.clientId"),
        stringFromEnv("github.clientSecret")
    ),

    )

private fun Application.stringFromEnv(path: String) =
    environment.config.property(path).getString()

data class ConfigurationProperties(
    val developmentMode: Boolean,
    val jwt: JWT,
    val adminAuth: AdminAuth,
    val cookies: Cookies,
    val db: DB,
    val githubCredentials: OAuthCredentials,
) {
    data class JWT(
        val secret: String,
        val audience: String,
        val realm: String,
        val issuer: String,
    )

    data class AdminAuth(
        val realm: String,
        val username: String,
        val password: String
    )

    data class Cookies(
        val encryptionKey: ByteArray,
        val signingKey: ByteArray
    )

    data class DB(
        val redis: String
    )

    data class OAuthCredentials(
        val clientId: String,
        val clientSecret: String
    )

}
