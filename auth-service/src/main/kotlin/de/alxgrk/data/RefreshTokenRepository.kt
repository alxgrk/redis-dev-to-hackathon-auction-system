package de.alxgrk.data

import de.alxgrk.authentication.RefreshToken
import de.alxgrk.plugins.JsonSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.search.IndexDefinition
import redis.clients.jedis.search.IndexOptions
import redis.clients.jedis.search.Schema
import java.util.*


@Serializable
@JvmInline
value class Days(private val value: Int) {
    fun expiryDateFromNow(now: Long = System.currentTimeMillis()) = ExpiryDate(now + (value * 24 * 60 * 60 * 1000))
}

val Int.days: Days
    get() = Days(this)

@Serializable
@JvmInline
value class ExpiryDate(val millis: Long)

@Serializable
data class RefreshTokenDocument(
    @Contextual val userId: UUID,
    @Contextual val refreshToken: UUID,
    val expiryDate: ExpiryDate
)

interface RefreshTokenRepository {
    suspend fun update(userId: UUID, refreshToken: RefreshToken, daysValid: Days): Boolean
    suspend fun getExpiryDateIfExists(refreshToken: RefreshToken): ExpiryDate?
    suspend fun invalidate(refreshToken: RefreshToken): Boolean
    suspend fun clearAllForUser(userId: UUID): Boolean
}

class RedisRefreshTokenRepository(private val jedis: JedisPooled) : RefreshTokenRepository {
    private val indexName = "refreshToken-index"
    private val prefix = "refreshTokens:"

    init {
        val schema: Schema = Schema()
            .addField(Schema.TextField("$.userId", 1.0, false, true))
            .addField(Schema.TextField("$.refreshToken", 1.0, false, true))
            .addField(Schema.TextField("$.expiryDate", .0, true, true))

        try {
            val rule = IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(prefix)
            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(rule), schema)
        } catch (e: JedisDataException) {
            if (e.message === "Index already exists") {
                jedis.ftAlter(indexName, schema)
            }
        }
    }

    override suspend fun update(userId: UUID, refreshToken: RefreshToken, daysValid: Days): Boolean {
        val refreshTokenDocument = RefreshTokenDocument(userId, refreshToken.value, daysValid.expiryDateFromNow())
        jedis.jsonSet("$prefix$userId", JsonSerializer.encodeToString(refreshTokenDocument))
        return true
    }

    override suspend fun getExpiryDateIfExists(refreshToken: RefreshToken): ExpiryDate? =
        jedis.jsonGet("$prefix${refreshToken.userId}", RefreshTokenDocument::class.java)?.expiryDate

    override suspend fun invalidate(refreshToken: RefreshToken): Boolean =
        jedis.jsonDel("$prefix${refreshToken.userId}") > 0

    override suspend fun clearAllForUser(userId: UUID): Boolean =
        jedis.jsonDel("$prefix${userId}") > 0

}
