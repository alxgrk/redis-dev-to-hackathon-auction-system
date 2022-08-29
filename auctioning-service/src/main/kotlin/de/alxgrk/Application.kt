package de.alxgrk

import de.alxgrk.bidding.initializeBiddingHandler
import de.alxgrk.common.MonetaryAmount
import de.alxgrk.common.SupportedCurrency
import de.alxgrk.data.AuctionDocument
import de.alxgrk.data.AuctionRepository
import de.alxgrk.data.ItemDocument
import de.alxgrk.data.ItemRepository
import io.ktor.server.netty.*
import de.alxgrk.plugins.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.kodein.di.instance
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Resources)

    configureDiAsync()
    configureSerialization()
    configureSecurity()
    configureSessions()
    configureWebsockets()
    configureRouting()
    configureMonitoring()
    configureHTTP()

    initializeBiddingHandler()

    dummyData()
}


fun Application.dummyData() {
    thread(isDaemon = true) {
        runBlocking {
            val auctionRepository: AuctionRepository by di.instance()
            val itemRepository: ItemRepository by di.instance()

            val item1 = ItemDocument(
                id = UUID.fromString("d6118270-8404-4560-bee9-50c32870dd69"),
                title = "Move Fast and Break Things",
                description = "Move Fast and Break Things",
                image = URL("https://imgs.xkcd.com/comics/move_fast_and_break_things_2x.png"),
                owner = UUID.fromString("735db4d6-658d-4ea6-aeed-d0718e076d6a")
            )
            itemRepository.upsert(item1)
            val item2 = ItemDocument(
                id = UUID.fromString("c448ca77-425d-4b3a-b780-9da0b1fdd3f0"),
                title = "Cat Proximity",
                description = "Cat Proximity",
                image = URL("https://imgs.xkcd.com/comics/cat_proximity.png"),
                owner = UUID.fromString("735db4d6-658d-4ea6-aeed-d0718e076d6a")
            )
            itemRepository.upsert(item2)
            val item3 = ItemDocument(
                id = UUID.fromString("a5306bfd-8aca-45a5-a15e-af25beddcb5f"),
                title = "UV",
                description = "UV",
                image = URL("https://imgs.xkcd.com/comics/uv_2x.png"),
                owner = UUID.fromString("a0925332-2805-4153-ade5-1108aa279e6a")
            )
            itemRepository.upsert(item3)

            val auction1 = AuctionDocument(
                id = UUID.fromString("4646b7c1-9196-42ce-98c1-4328995e7932"),
                start = Instant.parse("2022-08-24T22:53:20.625Z"),
                end = Instant.parse("2022-09-30T22:53:20.625Z"),
                items = listOf(
                    UUID.fromString("d6118270-8404-4560-bee9-50c32870dd69"),
                    UUID.fromString("c448ca77-425d-4b3a-b780-9da0b1fdd3f0")
                ),
                title = "Brand New Comics",
                description = "Two of the most recent xkcd comics. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Mattis pellentesque id nibh tortor id aliquet lectus proin nibh. Eget nunc scelerisque viverra mauris in aliquam. Etiam tempor orci eu lobortis elementum nibh tellus molestie nunc. Sit amet risus nullam eget felis.",
                seller = UUID.fromString("735db4d6-658d-4ea6-aeed-d0718e076d6a"),
                lowestBid = MonetaryAmount(
                    amount = 10.0,
                    currency = SupportedCurrency.EUR
                ),
                keywords = listOf("comics", "xkcd", "rare"),
                isClosed = false
            )
            auctionRepository.upsert(auction1)
        }
    }
}
