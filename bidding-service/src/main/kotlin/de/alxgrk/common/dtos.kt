package de.alxgrk.common

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BiddingRequest(
    val amount: Double,
    val currency: SupportedCurrency,
)

@Serializable
data class BiddingResponse(
    @Contextual override val id: UUID,
    override val timestamp: Instant,
    override val amount: Double,
    override val currency: SupportedCurrency,
    override val context: BiddingContext
) : Bid

fun Bid.toDto() = BiddingResponse(id, timestamp, amount, currency, context)
