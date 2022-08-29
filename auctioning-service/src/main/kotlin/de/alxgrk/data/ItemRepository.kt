package de.alxgrk.data

import de.alxgrk.common.*
import de.alxgrk.plugins.JsonSerializer
import kotlinx.serialization.*
import kotlinx.serialization.json.decodeFromJsonElement
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.search.*
import java.net.URL
import java.util.*
import kotlin.collections.List

@Serializable
data class ItemDocument(
    @Contextual override val id: UUID = UUID.randomUUID(),
    override val title: String,
    override val description: String,
    @Contextual override val image: URL,
    @Contextual override val owner: UUID
): Item

interface ItemRepository {
    suspend fun list(): List<Item>
    suspend fun upsert(item: ItemDocument): Item?
    suspend fun findById(itemId: UUID): Item?
    suspend fun findByOwner(ownerId: UUID): List<Item>
    suspend fun isOwnedBy(itemId: UUID, ownerId: UUID): Boolean
    suspend fun delete(itemId: UUID): Boolean
}

class RedisItemRepository(private val jedis: JedisPooled) : ItemRepository {
    private val indexName = "items-index"
    private val prefix = "items:"

    init {
        val schema: Schema = Schema()
            .addField(tag("id"))
            .addField(text("title"))
            .addField(text("description"))
            .addField(tag("image"))
            .addField(tag("owner"))

        try {
            val rule = IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(prefix)
            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(rule), schema)
        } catch (e: JedisDataException) {
            if (e.message === "Index already exists") {
                jedis.ftAlter(indexName, schema)
            }
        }
    }

    override suspend fun list(): List<ItemDocument> =
        jedis.ftSearch(
            indexName,
            Query("*")
        ).documents.map { JsonSerializer.decodeFromString(it.getString("$")) }

    override suspend fun upsert(item: ItemDocument): Item? {
        val existingItem = jedis.ftSearch(indexName, Query("@id:{${item.id.toRedisEscapedString()}}")).documents
            .map { JsonSerializer.decodeFromString<ItemDocument>(it.getString("$")) }
            .firstOrNull()
        jedis.jsonSet("$prefix${item.id}", JsonSerializer.encodeToString(item))
        return existingItem
    }

    override suspend fun findById(itemId: UUID): ItemDocument? =
        JsonSerializer.decodeFromJsonElement(
            jedis.jsonGet("$prefix$itemId", Map::class.java).toJsonElement()
        )

    override suspend fun findByOwner(ownerId: UUID): List<Item> =
        jedis.ftSearch(
            indexName,
            Query("@owner:{${ownerId.toRedisEscapedString()}}")
        ).documents.map { JsonSerializer.decodeFromString<ItemDocument>(it.getString("$")) }

    override suspend fun isOwnedBy(itemId: UUID, ownerId: UUID) = jedis.ftSearch(indexName, Query("@id:{${itemId.toRedisEscapedString()}} @owner:{${ownerId.toRedisEscapedString()}}")).documents.isNotEmpty()

    override suspend fun delete(itemId: UUID): Boolean =
        jedis.jsonDel("$prefix${itemId}") > 0

}
