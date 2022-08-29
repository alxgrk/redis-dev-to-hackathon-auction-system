package de.alxgrk.data

import de.alxgrk.config.ConfigurationProperties
import org.kodein.di.DI
import org.kodein.di.bindEagerSingleton
import redis.clients.jedis.JedisPooled

fun DI.MainBuilder.dataModule(
    configurationProperties: ConfigurationProperties
) {

    val jedis = JedisPooled(configurationProperties.db.redis)
    bindEagerSingleton<BiddingStream> { RedisBiddingStream(jedis) }
    bindEagerSingleton<BiddingRepository> { RedisBiddingRepository(jedis) }
    bindEagerSingleton<AuctionRepository> { RedisAuctionRepository(jedis) }
    bindEagerSingleton<ItemRepository> { RedisItemRepository(jedis) }
}
