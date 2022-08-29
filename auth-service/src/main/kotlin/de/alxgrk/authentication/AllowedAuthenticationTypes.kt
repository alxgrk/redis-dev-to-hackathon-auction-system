package de.alxgrk.authentication

import de.alxgrk.authentication.oauth.OAuthProvider


sealed class AllowedAuthenticationTypes(val key: String) {
    object AdminAuth : AllowedAuthenticationTypes("admin-auth")
    object JWTAuth : AllowedAuthenticationTypes("jwt-auth")
    class OAuth(provider: OAuthProvider) : AllowedAuthenticationTypes("oauth-${provider.name}")
}
