package de.alxgrk.data

import de.alxgrk.common.Bid
import de.alxgrk.common.BiddingContext
import de.alxgrk.common.SupportedCurrency
import de.alxgrk.plugins.JsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.params.XAddParams
import java.util.*

@Serializable
data class BidDocument(
    @Contextual override val id: UUID = UUID.randomUUID(),
    override val timestamp: Instant,
    override val amount: Double,
    override val currency: SupportedCurrency,
    override val context: BiddingContext
) : Bid

interface BiddingQueue {
    suspend fun publish(bid: Bid): Boolean
}

class RedisBiddingQueue(private val jedis: JedisPooled) : BiddingQueue {
    private val prefix = "bids:"

    override suspend fun publish(bid: Bid): Boolean {
        val bidDocument = BidDocument(
            id = bid.id,
            timestamp = bid.timestamp,
            amount = bid.amount,
            currency = bid.currency,
            context = bid.context
        )
        jedis.xadd("${prefix}all", mapOf("$" to JsonSerializer.encodeToString(bidDocument)), XAddParams())
        return true
    }

}
