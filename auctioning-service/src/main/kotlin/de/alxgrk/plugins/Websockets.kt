package de.alxgrk.plugins

import de.alxgrk.common.BidDto
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.*
import java.util.zip.Deflater
import kotlin.collections.LinkedHashMap

fun Application.configureWebsockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(JsonSerializer)
        extensions {
            install(WebSocketDeflateExtension) {
                compressionLevel = Deflater.DEFAULT_COMPRESSION
                compressIfBiggerThan(bytes = 4 * 1024)
            }
        }
    }
}

private val connections = Collections.synchronizedMap<UUID, List<WebSocketServerSession>>(LinkedHashMap())

suspend fun notifyAboutNewBid(auctionId: UUID, bids: List<BidDto>) = connections
    .firstNotNullOfOrNull { (key, value) -> if (auctionId == key) value else null }
    ?.forEach {
        it.sendSerialized(bids)
    }

fun Route.newBidsWebsocket() {

    val log = this@newBidsWebsocket.application.log

    webSocket(Auctions.ById.NewBids) {

            val auctionId = call.parameters["auctionId"]
                ?.let { UUID.fromString(it) }
                ?: throw IllegalArgumentException("Path param auctionId not present.")
            connections.computeIfPresent(auctionId) { _, oldValue ->
                oldValue.toMutableList().apply { add(this@webSocket) }
            }
            connections.computeIfAbsent(auctionId) { listOf(this) }

            log.info("New ws subscription for auction with id $auctionId")

            fun removeConnection() = connections.computeIfPresent(auctionId) { _, oldValue ->
                oldValue.toMutableList().apply { remove(this@webSocket) }
            }

            try {
                closeReason.invokeOnCompletion { cause ->
                    log.info("Ws subscription for auction with id $auctionId closed - reason: $cause")
                    removeConnection()
                }
                for (frame in incoming) {
                    frame as? Frame.Close ?: continue
                    log.debug("Client sent close frame - removing connection")
                    break
                }
            } catch (e: ClosedReceiveChannelException) {
                log.info("Ws subscription for auction with id $auctionId closed - reason: ${closeReason.await()}")
            } catch (e: Throwable) {
                log.error(
                    "Error in ws subscription for auction with id $auctionId - reason:  ${closeReason.await()}",
                    e
                )
            } finally {
                removeConnection()
                close()
            }

    }
}

private class CustomWebSocketServerSession(delegate: DefaultWebSocketSession, override val call: ApplicationCall) : DefaultWebSocketSession by delegate, WebSocketServerSession
