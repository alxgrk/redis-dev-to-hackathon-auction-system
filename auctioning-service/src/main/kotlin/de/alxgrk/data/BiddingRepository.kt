package de.alxgrk.data

import de.alxgrk.common.*
import de.alxgrk.plugins.JsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.json.Path
import redis.clients.jedis.json.Path2
import redis.clients.jedis.search.*
import java.util.*
import kotlin.collections.List

@Serializable
data class BidDocument(
    @Contextual val id: UUID,
    val timestamp: Instant,
    val amount: Double,
    val currency: SupportedCurrency,
    val context: BiddingContext
)

@Serializable
data class BiddingContext(
    @Contextual val userId: UUID,
    @Contextual val auctionId: UUID
)

interface BiddingRepository {
    suspend fun listBiddings(auctionId: UUID): List<BidDocument>
    suspend fun add(auctionId: UUID, vararg bidDocuments: BidDocument): List<BidDocument>
    suspend fun remove(auctionId: UUID, bidDocument: BidDocument): BidDocument?
}

class RedisBiddingRepository(private val jedis: JedisPooled) : BiddingRepository {
    private val prefix = "biddings:"

    override suspend fun listBiddings(auctionId: UUID): List<BidDocument> {
        val bids = jedis.jsonGet("$prefix$auctionId", List::class.java) ?: listOf<String>()
        return bids.map { JsonSerializer.decodeFromJsonElement<BidDocument>(it.toJsonElement()) }
            .sortedByDescending { it.timestamp }
    }

    override suspend fun add(auctionId: UUID, vararg bidDocuments: BidDocument): List<BidDocument> {
        val jsonBids = bidDocuments.map { JsonSerializer.encodeToString(it) }.toTypedArray()

        if (jedis.exists("$prefix${auctionId}")) {
            jedis.jsonArrAppend("$prefix${auctionId}", Path2.ROOT_PATH, *jsonBids)
        } else {
            val asJsonArray = "[${jsonBids.joinToString(",")}]"
            jedis.jsonSet("$prefix${auctionId}", Path2.ROOT_PATH, asJsonArray)
        }

        return bidDocuments.toList()
    }

    override suspend fun remove(auctionId: UUID, bidDocument: BidDocument): BidDocument? {
        val index = listBiddings(auctionId).indexOf(bidDocument)

        if (index == -1)
            return null

        val removedJson = jedis.jsonArrPop("$prefix$auctionId", Map::class.java, Path.ROOT_PATH, index).toJsonElement()
        return JsonSerializer.decodeFromJsonElement(removedJson)
    }

}
