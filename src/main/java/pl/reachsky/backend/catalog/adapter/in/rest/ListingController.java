package pl.reachsky.backend.catalog.adapter.in.rest;

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
import pl.reachsky.backend.catalog.application.port.in.CreateListingUseCase;
import pl.reachsky.backend.catalog.application.port.in.FindListingsQuery;
import pl.reachsky.backend.catalog.application.port.in.PublishListingUseCase;
import pl.reachsky.backend.catalog.application.port.in.RenewListingUseCase;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.NotFoundException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
class ListingController {

    private final CreateListingUseCase create;
    private final PublishListingUseCase publish;
    private final RenewListingUseCase renew;
    private final FindListingsQuery find;
    private final ListingWebMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    ListingController(CreateListingUseCase create, PublishListingUseCase publish,
                      RenewListingUseCase renew, FindListingsQuery find,
                      ListingWebMapper mapper, CurrentUserProvider currentUserProvider) {
        this.create = create;
        this.publish = publish;
        this.renew = renew;
        this.find = find;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new listing")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "SELLER role required")
    ListingResponse createListing(@Valid @RequestBody CreateListingRequest request) {
        UUID sellerId = currentUserProvider.get().id().value();
        return mapper.toResponse(create.create(mapper.toCommand(request, sellerId)));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a DRAFT listing")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "SELLER role required")
    @ApiResponse(responseCode = "409", description = "Listing is not in DRAFT state")
    ListingResponse publishListing(@PathVariable UUID id) {
        return mapper.toResponse(publish.publish(new ListingId(id)));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Re-activate a CLOSED listing")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "SELLER role required")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    @ApiResponse(responseCode = "409", description = "Listing is not in CLOSED state")
    ListingResponse renewListing(@PathVariable UUID id) {
        return mapper.toResponse(renew.renew(new ListingId(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing by ID")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    ListingResponse getById(@PathVariable UUID id) {
        return find.findById(new ListingId(id))
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id));
    }

    @GetMapping
    @Operation(summary = "List active listings whose resource window has not yet ended")
    List<ListingResponse> listActive() {
        return find.findAvailable().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/my")
    @Operation(summary = "List all listings belonging to the current seller (all statuses)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    List<ListingResponse> listMy() {
        UUID sellerId = currentUserProvider.get().id().value();
        return find.findBySeller(sellerId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
