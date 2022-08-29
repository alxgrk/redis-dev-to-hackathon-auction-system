package de.alxgrk.bidding

import de.alxgrk.common.toDto
import de.alxgrk.data.BiddingRepository
import de.alxgrk.data.BiddingStream
import de.alxgrk.plugins.di
import de.alxgrk.plugins.notifyAboutNewBid
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.instance
import kotlin.concurrent.thread

fun Application.initializeBiddingHandler() {
    thread(isDaemon = true) {
        runBlocking {
            val biddingStream: BiddingStream by di.instance()
            val biddingRepository: BiddingRepository by di.instance()

            while (true) {
                val (newLastId, bids) = biddingStream.subscribe(biddingStream.getLastId())
                biddingStream.storeNewLastId(newLastId)

                log.debug("Received ${bids.size} new bids.")

                bids.groupBy { it.context.auctionId }
                    .forEach { (auctionId, bids) ->
                        biddingRepository.add(auctionId, *bids.toTypedArray())
                        notifyAboutNewBid(auctionId, bids.map { it.toDto() })
                    }
            }
        }
    }
}
