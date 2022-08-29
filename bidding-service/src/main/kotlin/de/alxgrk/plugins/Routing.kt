package de.alxgrk.plugins

import de.alxgrk.authentication.AllowedAuthenticationTypes
import de.alxgrk.authentication.userId
import de.alxgrk.common.BiddingContext
import de.alxgrk.common.BiddingRequest
import de.alxgrk.data.BidDocument
import de.alxgrk.data.BiddingQueue
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.util.logging.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.kodein.di.instance
import java.util.UUID

fun Application.configureRouting() {

    val biddingQueue: BiddingQueue by di.instance()

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Unauthorized, cause.toErrorMessage())
        }
        exception<AuthorizationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Forbidden, cause.toErrorMessage())
        }
        exception<NotFoundException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.NotFound, cause.toErrorMessage())
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.BadRequest, ErrorMessage("Bad Request"))
        }
    }

    routing {
        authenticate(AllowedAuthenticationTypes.JWTAuth.key) {
            post<Bidding> { bidding ->
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val biddingRequest = call.receive<BiddingRequest>()
                val auctionId = UUID.fromString(bidding.auctionId)

                val bid = BidDocument(
                    timestamp = Clock.System.now(),
                    amount = biddingRequest.amount,
                    currency = biddingRequest.currency,
                    context = BiddingContext(
                        userId = userId,
                        auctionId = auctionId
                    )
                )
                val wasSuccessful = biddingQueue.publish(bid)
                if (wasSuccessful)
                    call.respond(HttpStatusCode.Created, "")
                else
                    call.respond(HttpStatusCode.BadRequest, "")
            }
        }
    }
}

@Serializable
@Resource("/auctions/{auctionId}/bidding")
private class Bidding(val auctionId: String)

@Serializable
class ErrorMessage(val message: String)
class AuthenticationException(message: String? = null) : RuntimeException(message ?: "")
class AuthorizationException(message: String) : RuntimeException(message)

fun Exception.toErrorMessage() = message?.let { ErrorMessage(it) } ?: emptyMap<String, String>()
