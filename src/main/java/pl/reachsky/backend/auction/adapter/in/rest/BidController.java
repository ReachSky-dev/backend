package pl.reachsky.backend.auction.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.reachsky.backend.auction.application.port.in.BidResult;
import pl.reachsky.backend.auction.application.port.in.FindBidsQuery;
import pl.reachsky.backend.auction.application.port.in.PlaceBidCommand;
import pl.reachsky.backend.auction.application.port.in.PlaceBidUseCase;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidCommand;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidUseCase;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.Money;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
class BidController {

    private final PlaceBidUseCase placeBid;
    private final SetProxyBidUseCase setProxyBid;
    private final FindBidsQuery findBids;
    private final CurrentUserProvider currentUserProvider;

    BidController(PlaceBidUseCase placeBid, SetProxyBidUseCase setProxyBid,
                  FindBidsQuery findBids, CurrentUserProvider currentUserProvider) {
        this.placeBid = placeBid;
        this.setProxyBid = setProxyBid;
        this.findBids = findBids;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a bid on an English auction")
    @ApiResponse(responseCode = "409", description = "Bid rejected — too low or auction not running")
    @ApiResponse(responseCode = "200", description = "Duplicate idempotency key — returns existing bid")
    BidResponse placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceBidRequest request) {
        UUID callerId = currentUserProvider.get().id().value();
        PlaceBidCommand cmd = new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(callerId),
                new Money(request.amountInMinorUnits(), java.util.Currency.getInstance(request.currency())),
                idempotencyKey);
        BidResult result = placeBid.place(cmd);
        Bid bid = switch (result) {
            case BidResult.Accepted a -> a.bid();
            case BidResult.Duplicate d -> d.bid();
        };
        return toResponse(bid);
    }

    @PostMapping("/proxy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set or update a proxy (automatic) bid")
    void setProxy(
            @PathVariable UUID auctionId,
            @Valid @RequestBody SetProxyBidRequest request) {
        UUID callerId = currentUserProvider.get().id().value();
        setProxyBid.set(new SetProxyBidCommand(
                new AuctionId(auctionId),
                new BidderId(callerId),
                new Money(request.maxAmountInMinorUnits(),
                        java.util.Currency.getInstance(request.currency()))));
    }

    @GetMapping
    @Operation(summary = "List all bids for an auction, ordered by sequence ascending")
    List<BidResponse> listBids(@PathVariable UUID auctionId) {
        return findBids.findByAuction(new AuctionId(auctionId))
                .stream().map(this::toResponse).toList();
    }

    private BidResponse toResponse(Bid b) {
        return new BidResponse(
                b.getId().value(),
                b.getAuctionId().value(),
                b.getBidderId().value(),
                b.getAmount().amountInMinorUnits(),
                b.getAmount().currency().getCurrencyCode(),
                b.getSequence(),
                b.getPlacedAt());
    }
}
