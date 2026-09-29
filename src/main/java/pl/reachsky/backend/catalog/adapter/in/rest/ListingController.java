package pl.reachsky.backend.catalog.adapter.in.rest;

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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/listings")
class ListingController {

    private final CreateListingUseCase create;
    private final PublishListingUseCase publish;
    private final FindListingsQuery find;
    private final ListingWebMapper mapper;

    ListingController(CreateListingUseCase create, PublishListingUseCase publish,
                      FindListingsQuery find, ListingWebMapper mapper) {
        this.create = create;
        this.publish = publish;
        this.find = find;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ListingResponse createListing(@Valid @RequestBody CreateListingRequest request) {
        return mapper.toResponse(create.create(mapper.toCommand(request)));
    }

    @PostMapping("/{id}/publish")
    ListingResponse publishListing(@PathVariable UUID id) {
        return mapper.toResponse(publish.publish(new ListingId(id)));
    }

    @GetMapping
    List<ListingResponse> listActive() {
        return find.findByStatus(ListingStatus.ACTIVE).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
