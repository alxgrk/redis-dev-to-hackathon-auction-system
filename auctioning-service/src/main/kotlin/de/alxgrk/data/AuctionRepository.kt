package de.alxgrk.data

import de.alxgrk.common.*
import de.alxgrk.plugins.JsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.decodeFromJsonElement
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.json.Path
import redis.clients.jedis.search.*
import java.util.*
import kotlin.collections.List

@Serializable
data class AuctionDocument(
    @Contextual override val id: UUID = UUID.randomUUID(),
    override val start: Instant,
    override val end: Instant,
    override val items: List<@Contextual UUID>,
    override val title: String,
    override val description: String,
    override val seller: @Contextual UUID,
    override val lowestBid: MonetaryAmount,
    override val keywords: List<String>? = listOf(),
    override val isClosed: Boolean
) : Auction

interface AuctionRepository {
    suspend fun list(itemIds: List<UUID>?): List<Auction>
    suspend fun upsert(auction: AuctionDocument): Auction?
    suspend fun findById(auctionId: UUID): Auction?
    suspend fun findIdsByItemId(itemId: UUID): List<UUID>
    suspend fun findBySeller(sellerId: UUID): List<Auction>
    suspend fun isOwnedBy(auctionId: UUID, sellerId: UUID): Boolean
    suspend fun isClosed(auctionId: UUID): Boolean
    suspend fun close(auctionId: UUID)
}

class RedisAuctionRepository(private val jedis: JedisPooled) : AuctionRepository {
    private val indexName = "auctions-index"
    private val prefix = "auctions:"

    init {
        val schema: Schema = Schema()
            .addField(tag("id"))
            .addField(tag("start"))
            .addField(tag("end"))
            .addField(tagArray("items"))
            .addField(text("title"))
            .addField(text("description"))
            .addField(tag("seller"))
            .addMonetaryAmountField("lowestBid")
            .addField(tagArray("keywords"))
            .addField(tag("isClosed"))

        try {
            val rule = IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(prefix)
            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(rule), schema)
        } catch (e: JedisDataException) {
            if (e.message === "Index already exists") {
                jedis.ftAlter(indexName, schema)
            }
        }
    }

    override suspend fun list(itemIds: List<UUID>?): List<Auction> {
        val queryString = itemIds
            ?.joinToString(" | ") { it.toRedisEscapedString() }
            ?.let { "@items:{$it}" }
            ?: "*"
        return jedis.ftSearch(
            indexName,
            Query(queryString)
        ).documents.map { JsonSerializer.decodeFromString<AuctionDocument>(it.getString("$")) }
    }

    override suspend fun upsert(auction: AuctionDocument): Auction? {
        val existingAuction = jedis.ftSearch(indexName, Query("@id:{${auction.id.toRedisEscapedString()}}")).documents
            .map { JsonSerializer.decodeFromString<AuctionDocument>(it.getString("$")) }
            .firstOrNull()
        jedis.jsonSet("$prefix${auction.id}", JsonSerializer.encodeToString(auction))
        return existingAuction
    }

    override suspend fun findById(auctionId: UUID): Auction? =
        JsonSerializer.decodeFromJsonElement<AuctionDocument?>(
            jedis.jsonGet("$prefix$auctionId", Map::class.java).toJsonElement()
        )

    override suspend fun findIdsByItemId(itemId: UUID): List<UUID> =
        jedis.ftSearch(indexName, Query("@items:{${itemId.toRedisEscapedString()}}").setNoContent()).documents
            .map { UUID.fromString(it.id.removePrefix(prefix)) }

    override suspend fun findBySeller(sellerId: UUID): List<Auction> =
        jedis.ftSearch(
            indexName,
            Query("@seller:{${sellerId.toRedisEscapedString()}}")
        ).documents.map { JsonSerializer.decodeFromString<AuctionDocument>(it.getString("$")) }

    override suspend fun isOwnedBy(auctionId: UUID, sellerId: UUID) = jedis.ftSearch(
        indexName,
        Query("@id:{${auctionId.toRedisEscapedString()}} @seller:{${sellerId.toRedisEscapedString()}}")
    ).documents.isNotEmpty()

    override suspend fun isClosed(auctionId: UUID) = jedis.ftSearch(
        indexName,
        Query("@id:{${auctionId.toRedisEscapedString()}} @closed:{true}")
    ).documents.isNotEmpty()

    override suspend fun close(auctionId: UUID) {
        jedis.jsonSet("$prefix${auctionId}", Path.of(".isClosed"), true)
    }

}
