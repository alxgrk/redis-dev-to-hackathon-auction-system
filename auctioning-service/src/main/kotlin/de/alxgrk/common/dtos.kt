package de.alxgrk.common

import de.alxgrk.data.BidDocument
import de.alxgrk.data.BiddingContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.net.URL
import java.util.UUID

@Serializable
data class CreateAuctionRequest(
    val start: Instant,
    val end: Instant,
    val items: List<@Contextual UUID>,
    val title: String,
    val description: String,
    val lowestBid: MonetaryAmount,
    val keywords: List<String>?,
)

@Serializable
data class UpdateAuctionRequest(
    val id: @Contextual UUID,
    val start: Instant,
    val end: Instant,
    val items: List<@Contextual UUID>,
    val title: String,
    val description: String,
    val lowestBid: MonetaryAmount,
    val keywords: List<String>?,
)


@Serializable
data class AuctionResponse(
    val id: @Contextual UUID,
    val start: Instant,
    val end: Instant,
    val items: List<@Contextual UUID>,
    val title: String,
    val description: String,
    val seller: @Contextual UUID,
    val lowestBid: MonetaryAmount,
    val keywords: List<String>?,
    val isClosed: Boolean,
    val bids: List<BidDto>
) {
    companion object {
        fun from(auction: Auction, bidDocuments: List<BidDocument>) =
            AuctionResponse(
                id = auction.id,
                start = auction.start,
                end = auction.end,
                items = auction.items,
                title = auction.title,
                description = auction.description,
                seller = auction.seller,
                lowestBid = auction.lowestBid,
                keywords = auction.keywords,
                isClosed = auction.isClosed,
                bids = bidDocuments.map { it.toDto() }
                    // intentionally sorted by amount to get increasing bids
                    .sortedByDescending { it.amount }
            )
    }
}


@Serializable
data class CreateItemRequest(
    val title: String,
    val description: String,
    @Contextual val image: URL,
)

@Serializable
data class UpdateItemRequest(
    val id: @Contextual UUID,
    val title: String,
    val description: String,
    @Contextual val image: URL,
)

@Serializable
data class ItemResponse(
    @Contextual val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String,
    @Contextual val image: URL,
    @Contextual val owner: UUID,
    val auctions: List<@Contextual UUID>
) {
    companion object {
        fun from(item: Item, auctionIds: List<UUID>) =
            ItemResponse(
                id = item.id,
                title = item.title,
                description = item.description,
                image = item.image,
                owner = item.owner,
                auctions = auctionIds,
            )
    }
}

@Serializable
data class BidDto(
    @Contextual val id: UUID,
    val timestamp: Instant,
    val amount: Double,
    val currency: SupportedCurrency,
    val bidder: @Contextual UUID
)

fun BidDocument.toDto() = BidDto(
    id = this.id,
    timestamp = this.timestamp,
    amount = this.amount,
    currency = this.currency,
    bidder = this.context.userId
)
