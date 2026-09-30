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
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;
import pl.reachsky.backend.shared.CurrentUserProvider;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
class ListingController {

    private final CreateListingUseCase create;
    private final PublishListingUseCase publish;
    private final FindListingsQuery find;
    private final ListingWebMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    ListingController(CreateListingUseCase create, PublishListingUseCase publish,
                      FindListingsQuery find, ListingWebMapper mapper,
                      CurrentUserProvider currentUserProvider) {
        this.create = create;
        this.publish = publish;
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

    @GetMapping
    @Operation(summary = "List all active listings")
    List<ListingResponse> listActive() {
        return find.findByStatus(ListingStatus.ACTIVE).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
