package de.alxgrk

import de.alxgrk.common.BiddingContext
import de.alxgrk.common.SupportedCurrency
import de.alxgrk.data.BidDocument
import de.alxgrk.data.BiddingQueue
import io.ktor.server.netty.*
import de.alxgrk.plugins.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.kodein.di.instance
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextULong

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Resources)

    configureDiAsync()
    configureSerialization()
    configureSecurity()
    configureSessions()
    configureRouting()
    configureMonitoring()
    configureHTTP()

    dummyData()
}

fun Application.dummyData() {
    thread(isDaemon = true) {
        runBlocking {
            val biddingQueue: BiddingQueue by di.instance()

            val bidders = listOf("f76f89f0-aacf-4cb5-ab1a-4b7969364725", "a0925332-2805-4153-ade5-1108aa279e6a")
                .map { UUID.fromString(it) }

            while (true) {
                val bid = BidDocument(
                    timestamp = Clock.System.now(),
                    amount = Random.nextDouble(1.0, 999.0).let { "%.2f".format(Locale.US, it).toDouble() },
                    currency = SupportedCurrency.EUR,
                    context = BiddingContext(
                        auctionId = UUID.fromString("4646b7c1-9196-42ce-98c1-4328995e7932"),
                        userId = bidders.random()
                    )
                )
                biddingQueue.publish(bid)
                delay(Random.nextULong((10 * 60 * 1000).toULong(), (2 * 60 * 60 * 1000).toULong()).toLong())
            }
        }
    }
}
