package de.alxgrk.data

import de.alxgrk.common.AuthType
import de.alxgrk.common.User
import de.alxgrk.plugins.JsonSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.json.Path
import redis.clients.jedis.search.*
import java.util.*

@Serializable
data class UserDocument(
    @Contextual override val id: UUID = UUID.randomUUID(),
    override val externalId: String,
    override val login: String,
    override val name: String,
    override val email: String,
    override val authType: AuthType,
    override val organization: String? = null,
    override val homepageUrl: String? = null,
    override val avatarUrl: String? = null,
    override val address: Address? = null,
    override val paymentInfo: PaymentInfo? = null,
) : User

@Serializable
data class Address(
    val street: String,
    val houseNumber: String,
    val zipCode: String,
    val city: String,
    val country: String
)

@Serializable
data class PaymentInfo(
    val creditCardNumber: String,
)

interface UserRepository {
    suspend fun findAll(): List<User>
    suspend fun insert(user: User): Boolean
    suspend fun findById(userId: UUID): User?
    suspend fun findByExternalId(externalId: String): User?
    suspend fun updateAddress(userId: UUID, address: Address): User?
    suspend fun updatePaymentInfo(userId: UUID, paymentInfo: PaymentInfo): User?
    suspend fun deleteById(userId: UUID): Boolean
}

class RedisUserRepository(private val jedis: JedisPooled) : UserRepository {
    private val indexName = "users-index"
    private val prefix = "users:"

    init {
        val schema: Schema = Schema()
            .addField(tag("id"))
            .addField(tag("externalId"))
            .addField(text("login"))
            .addField(text("name"))
            .addField(text("email"))
            .addField(tag("authType"))
            .addField(text("organization"))
            .addField(tag("homepageUrl"))
            .addField(text("address.street"))
            .addField(tag("address.houseNumber"))
            .addField(tag("address.zipCode"))
            .addField(text("address.city"))
            .addField(tag("address.country"))
            .addField(tag("paymentInfo.creditCardNumber"))

        try {
            val rule = IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(prefix)
            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(rule), schema)
        } catch (e: JedisDataException) {
            if (e.message === "Index already exists") {
                jedis.ftAlter(indexName, schema)
            }
        }
    }

    override suspend fun findAll(): List<User> =
        jedis.ftSearch(indexName, Query("*")).documents.map { JsonSerializer.decodeFromString<UserDocument>(it.getString("$")) }

    override suspend fun insert(user: User): Boolean {
        var userDocument = UserDocument(
            id = user.id,
            externalId = user.externalId,
            login = user.login,
            name = user.name,
            email = user.email,
            authType = user.authType,
            organization = user.organization,
            homepageUrl = user.homepageUrl,
            avatarUrl = user.avatarUrl,
            address = null,
            paymentInfo = null
        )
        val existingUser = jedis.ftSearch(indexName, Query("@externalId:{${user.externalId.toRedisEscapedString()}}")).documents
            .map { JsonSerializer.decodeFromString<UserDocument>(it.getString("$")) }
            .firstOrNull()
        if (existingUser != null) {
            userDocument = userDocument.copy(id = existingUser.id)
        }
        jedis.jsonSet("$prefix${userDocument.id}", JsonSerializer.encodeToString(userDocument))
        return true
    }

    override suspend fun findById(userId: UUID): User? =
        jedis.jsonGet("$prefix$userId", UserDocument::class.java)

    override suspend fun findByExternalId(externalId: String): User? =
        jedis.ftSearch(indexName, Query("@externalId:{${externalId.toRedisEscapedString()}}"))
            .documents.firstOrNull()?.let { JsonSerializer.decodeFromString<UserDocument>(it.getString("$")) }

    override suspend fun updateAddress(userId: UUID, address: Address): User? {
        val user = jedis.jsonGet("$prefix$userId", UserDocument::class.java)
            ?: return null

        jedis.jsonSet("$prefix$userId", Path.of("$.address"), JsonSerializer.encodeToString(address))
        return user.copy(address = address)
    }

    override suspend fun updatePaymentInfo(userId: UUID, paymentInfo: PaymentInfo): User? {
        val user = jedis.jsonGet("$prefix$userId", UserDocument::class.java)
            ?: return null

        jedis.jsonSet("$prefix$userId", Path.of("$.paymentInfo"), JsonSerializer.encodeToString(paymentInfo))
        return user.copy(paymentInfo = paymentInfo)
    }

    override suspend fun deleteById(userId: UUID): Boolean =
        jedis.jsonDel("$prefix$userId") > 0


}
