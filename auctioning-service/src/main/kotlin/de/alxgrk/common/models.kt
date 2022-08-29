package de.alxgrk.common

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.net.URL
import java.util.UUID

interface Auction {
    val id: UUID
    val start: Instant
    val end: Instant
    val items: List<UUID>
    val title: String
    val description: String
    val seller: UUID
    val lowestBid: MonetaryAmount
    val keywords: List<String>?
    val isClosed: Boolean
}

@Serializable
data class MonetaryAmount(
    val amount: Double,
    val currency: SupportedCurrency
)

@Serializable
enum class SupportedCurrency(val symbol: Char) {
    USD('$'),
    EUR('€')
}

interface Item {
    val id: UUID
    val title: String
    val description: String
    val image: URL
    val owner: UUID
}
