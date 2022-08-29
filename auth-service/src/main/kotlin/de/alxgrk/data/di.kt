package de.alxgrk.data

import de.alxgrk.config.ConfigurationProperties
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.bindEagerSingleton
import redis.clients.jedis.JedisPooled
import java.util.*

fun DI.MainBuilder.dataModule(
    configurationProperties: ConfigurationProperties
) {

    val jedis = JedisPooled(configurationProperties.db.redis)
    bindEagerSingleton<RefreshTokenRepository> { RedisRefreshTokenRepository(jedis) }
    bindEagerSingleton<UserRepository> { RedisUserRepository(jedis) }
}
