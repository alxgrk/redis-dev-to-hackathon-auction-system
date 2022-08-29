package de.alxgrk.plugins

import de.alxgrk.authentication.AllowedAuthenticationTypes
import de.alxgrk.authentication.userId
import de.alxgrk.common.*
import de.alxgrk.data.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kodein.di.instance
import java.util.UUID
import java.util.zip.Deflater

fun Application.configureRouting() {

    val auctionRepository: AuctionRepository by di.instance()
    val biddingRepository: BiddingRepository by di.instance()
    val itemRepository: ItemRepository by di.instance()

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Unauthorized, cause.toErrorMessage())
        }
        exception<AuthorizationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Forbidden, cause.toErrorMessage())
        }
        exception<OnlyOwnerException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Forbidden, cause.toErrorMessage())
        }
        exception<NotFoundException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.NotFound, cause.toErrorMessage())
        }
        exception<AuctionClosedException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Conflict, cause.toErrorMessage())
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.BadRequest, ErrorMessage("Bad Request"))
        }
    }

    routing {
        authenticate(AllowedAuthenticationTypes.JWTAuth.key) {
            get<Auctions> {it ->
                val itemIds = it.items?.split(',')?.map { UUID.fromString(it) }
                val auctions = auctionRepository.list(itemIds)
                    .map {
                        AuctionResponse.from(it, biddingRepository.listBiddings(it.id))
                    }
                call.respond(HttpStatusCode.OK, auctions)
            }
            post<Auctions> {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val auctionRequest = call.receive<CreateAuctionRequest>()

                val auction = AuctionDocument(
                    start = auctionRequest.start,
                    end = auctionRequest.end,
                    items = auctionRequest.items,
                    title = auctionRequest.title,
                    description = auctionRequest.description,
                    seller = userId,
                    lowestBid = auctionRequest.lowestBid,
                    keywords = auctionRequest.keywords ?: listOf(),
                    isClosed = false
                )
                auctionRepository.upsert(auction)
                call.respond(HttpStatusCode.Created, AuctionResponse.from(auction, listOf()))
            }
            get<Auctions.ById> { auctions ->
                val auctionId = UUID.fromString(auctions.auctionId)

                val auction = auctionRepository.findById(auctionId)
                    ?: throw NotFoundException("Couldn't find auction with id ${auctions.auctionId}.")
                val biddings = biddingRepository.listBiddings(auctionId)

                call.respond(HttpStatusCode.OK, AuctionResponse.from(auction, biddings))
            }
            get<Auctions.My> {it ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val auctions = auctionRepository.findBySeller(userId)
                    .map {
                        AuctionResponse.from(it, biddingRepository.listBiddings(it.id))
                    }
                call.respond(HttpStatusCode.OK, auctions)
            }
            put<Auctions.ById> { auctions ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val auctionRequest = call.receive<UpdateAuctionRequest>()

                if (auctionRequest.id.toString() != auctions.auctionId) {
                    throw IllegalArgumentException("Path parameter doesn't match body.")
                }

                if (!auctionRepository.isOwnedBy(auctionRequest.id, userId)) {
                    throw OnlyOwnerException(userId)
                }

                if (auctionRepository.isClosed(auctionRequest.id)) {
                    throw AuctionClosedException(auctionRequest.id)
                }

                val auction = AuctionDocument(
                    id = auctionRequest.id,
                    start = auctionRequest.start,
                    end = auctionRequest.end,
                    items = auctionRequest.items,
                    title = auctionRequest.title,
                    description = auctionRequest.description,
                    seller = userId,
                    lowestBid = auctionRequest.lowestBid,
                    keywords = auctionRequest.keywords ?: listOf(),
                    isClosed = false
                )
                val oldAuction = auctionRepository.upsert(auction)
                val biddings = biddingRepository.listBiddings(auction.id)

                call.respond(HttpStatusCode.OK, AuctionResponse.from(oldAuction ?: auction, biddings))
            }
            delete<Auctions.ById> { auctions ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val auctionId = UUID.fromString(auctions.auctionId)

                if (!auctionRepository.isOwnedBy(auctionId, userId)) {
                    throw OnlyOwnerException(userId)
                }

                if (auctionRepository.isClosed(auctionId)) {
                    throw AuctionClosedException(auctionId)
                }

                auctionRepository.close(auctionId)
                call.respond(HttpStatusCode.NoContent)
            }


            get<Items> {
                val items = itemRepository.list()

                val filteredItems = call.request.queryParameters["search"]?.let { searchPhrase ->
                    items.filter { it.title.contains(searchPhrase) || it.description.contains(searchPhrase) }
                } ?: items

                val responses = filteredItems.map {
                    val auctionIds = auctionRepository.findIdsByItemId(it.id)
                    ItemResponse.from(it, auctionIds)
                }
                call.respond(HttpStatusCode.OK, responses)
            }
            post<Items> {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val itemRequest = call.receive<CreateItemRequest>()

                val item = ItemDocument(
                    title = itemRequest.title,
                    description = itemRequest.description,
                    image = itemRequest.image,
                    owner = userId,
                )
                itemRepository.upsert(item)

                val auctionIds = auctionRepository.findIdsByItemId(item.id)

                call.respond(HttpStatusCode.Created, ItemResponse.from(item, auctionIds))
            }
            get<Items.ById> { items ->
                val itemId = UUID.fromString(items.itemId)

                val item = itemRepository.findById(itemId)
                    ?: throw NotFoundException("Couldn't find item with id ${items.itemId}.")

                val auctionIds = auctionRepository.findIdsByItemId(itemId)

                call.respond(HttpStatusCode.OK, ItemResponse.from(item, auctionIds))
            }
            get<Items.My> { items ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val items = itemRepository.findByOwner(userId)

                val responses = items.map {
                    val auctionIds = auctionRepository.findIdsByItemId(it.id)
                    ItemResponse.from(it, auctionIds)
                }
                call.respond(HttpStatusCode.OK, responses)
            }
            put<Items.ById> { items ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val itemRequest = call.receive<UpdateItemRequest>()

                if (itemRequest.id.toString() != items.itemId) {
                    throw IllegalArgumentException("Path parameter doesn't match body.")
                }

                if (!itemRepository.isOwnedBy(itemRequest.id, userId)) {
                    throw OnlyOwnerException(userId)
                }

                val item = ItemDocument(
                    id = itemRequest.id,
                    title = itemRequest.title,
                    description = itemRequest.description,
                    image = itemRequest.image,
                    owner = userId,
                )
                val oldItem = itemRepository.upsert(item)
                val auctionIds = auctionRepository.findIdsByItemId(itemRequest.id)

                call.respond(HttpStatusCode.OK, ItemResponse.from(oldItem ?: item, auctionIds))
            }
            delete<Items.ById> { items ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val itemId = UUID.fromString(items.itemId)

                if (!itemRepository.isOwnedBy(itemId, userId)) {
                    throw OnlyOwnerException(userId)
                }

                itemRepository.delete(itemId)
                call.respond(HttpStatusCode.NoContent)
            }

            newBidsWebsocket()
        }
    }
}

@Serializable
@Resource("/auctions")
class Auctions(val items: String? = null) {

    @Serializable
    @Resource("/my")
    class My(val parent: Auctions = Auctions())

    @Serializable
    @Resource("/{auctionId}")
    class ById(val parent: Auctions = Auctions(), val auctionId: String) {

        companion object {
            const val NewBids = "/auctions/{auctionId}/newBids"
        }

    }
}

@Serializable
@Resource("/items")
class Items {

    @Serializable
    @Resource("/my")
    class My(val parent: Items = Items())

    @Serializable
    @Resource("/{itemId}")
    class ById(val parent: Items = Items(), val itemId: String)

}

@Serializable
class ErrorMessage(val message: String)
class AuthenticationException(message: String? = null) : RuntimeException(message ?: "")
class AuthorizationException(message: String) : RuntimeException(message)
class OnlyOwnerException(notOwner: UUID) :
    RuntimeException("User with id $notOwner is not allowed to execute the operation.")

class AuctionClosedException(auctionId: UUID) :
    RuntimeException("Auction with id $auctionId does not accept changes anymore.")

fun Exception.toErrorMessage() = message?.let { ErrorMessage(it) } ?: emptyMap<String, String>()
