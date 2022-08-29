package de.alxgrk.common

import de.alxgrk.data.Address
import de.alxgrk.data.PaymentInfo
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

interface User {
    val id: UUID
    val externalId: String
    val login: String
    val name: String
    val email: String
    val authType: AuthType
    val organization: String?
    val homepageUrl: String?
    val avatarUrl: String?
    val address: Address?
    val paymentInfo: PaymentInfo?
}

enum class AuthType {
    Github,
}
