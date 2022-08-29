package de.alxgrk.data

import de.alxgrk.plugins.JsonSerializer
import kotlinx.serialization.decodeFromString
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.StreamEntryID
import redis.clients.jedis.params.XReadParams

interface BiddingStream {
    fun getLastId(): StreamEntryID?
    fun storeNewLastId(id: StreamEntryID)
    fun subscribe(lastId: StreamEntryID?): Pair<StreamEntryID, List<BidDocument>>
}

class RedisBiddingStream(private val jedis: JedisPooled) : BiddingStream {
    private val prefix = "bids:"

    private var lastId: StreamEntryID? = null

    override fun getLastId(): StreamEntryID? = lastId ?: jedis.get("${prefix}lastId")?.let { StreamEntryID(it) }

    override fun storeNewLastId(id: StreamEntryID) {
        lastId = id
        jedis.set("${prefix}lastId", id.toString())
    }

    override fun subscribe(lastId: StreamEntryID?): Pair<StreamEntryID, List<BidDocument>> {
        val id = lastId ?: StreamEntryID()
        val entries = jedis.xread(XReadParams.xReadParams().block(0), mapOf("${prefix}all" to id))
        val (_, elements) = entries.first()
        val lastElementId = elements.last().id
        return lastElementId to elements.map { JsonSerializer.decodeFromString(it.fields["$"]!!) }
    }

}
