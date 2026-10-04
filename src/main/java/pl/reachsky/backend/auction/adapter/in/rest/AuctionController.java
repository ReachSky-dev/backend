package pl.reachsky.backend.auction.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.reachsky.backend.auction.application.port.in.CancelAuctionUseCase;
import pl.reachsky.backend.auction.application.port.in.CreateAuctionUseCase;
import pl.reachsky.backend.auction.application.port.in.FindAuctionsQuery;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.NotFoundException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
class AuctionController {

    private final CreateAuctionUseCase create;
    private final CancelAuctionUseCase cancel;
    private final FindAuctionsQuery find;
    private final AuctionWebMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    AuctionController(CreateAuctionUseCase create, CancelAuctionUseCase cancel,
                      FindAuctionsQuery find, AuctionWebMapper mapper,
                      CurrentUserProvider currentUserProvider) {
        this.create = create;
        this.cancel = cancel;
        this.find = find;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create and schedule a new auction for an active listing")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "SELLER role required")
    AuctionResponse createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        UUID sellerId = currentUserProvider.get().id().value();
        AuctionId id = create.create(mapper.toCommand(request, sellerId));
        return find.findById(id).map(mapper::toResponse)
                .orElseThrow();
    }

    @GetMapping
    @Operation(summary = "List all running auctions")
    List<AuctionResponse> listRunning() {
        return find.findRunning().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/my")
    @Operation(summary = "List all auctions created by the current seller (all statuses)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    List<AuctionResponse> listMy() {
        UUID sellerId = currentUserProvider.get().id().value();
        return find.findBySeller(sellerId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/history")
    @Operation(summary = "List all ended auctions (SOLD, RESERVE_NOT_MET, CANCELLED, SETTLED)")
    List<AuctionResponse> listHistory() {
        return find.findEnded().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/won")
    @Operation(summary = "List auctions won by the current user")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    List<AuctionResponse> listWon() {
        UUID buyerId = currentUserProvider.get().id().value();
        return find.findWon(buyerId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get auction by ID")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    AuctionResponse getById(@PathVariable UUID id) {
        return find.findById(new AuctionId(id))
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Auction not found: " + id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an auction")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "SELLER role required")
    @ApiResponse(responseCode = "409", description = "Auction cannot be cancelled in its current state")
    AuctionResponse cancelAuction(@PathVariable UUID id) {
        UUID callerId = currentUserProvider.get().id().value();
        cancel.cancel(new AuctionId(id), callerId);
        return find.findById(new AuctionId(id)).map(mapper::toResponse).orElseThrow();
    }
}
