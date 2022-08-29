package de.alxgrk.common

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

interface Bid {
    val id: UUID
    val timestamp: Instant
    val amount: Double
    val currency: SupportedCurrency
    val context: BiddingContext
}

@Serializable
data class BiddingContext(
    @Contextual val userId: UUID,
    @Contextual val auctionId: UUID
)

@Serializable
enum class SupportedCurrency(val symbol: Char) {
    USD('$'),
    EUR('€')
}
