package de.alxgrk.authentication

sealed class AllowedAuthenticationTypes(val key: String) {
    object AdminAuth : AllowedAuthenticationTypes("admin-auth")
    object JWTAuth : AllowedAuthenticationTypes("jwt-auth")
}
