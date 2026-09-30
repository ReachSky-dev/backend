package pl.reachsky.backend.identity.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.reachsky.backend.shared.CurrentUser;
import pl.reachsky.backend.shared.CurrentUserProvider;

@RestController
@RequestMapping("/api/me")
class MeController {

    private final CurrentUserProvider currentUserProvider;

    MeController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "Current authenticated user profile")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    UserProfileResponse getMe() {
        CurrentUser user = currentUserProvider.get();
        return new UserProfileResponse(
                user.id().value(),
                user.username(),
                user.roles(),
                null);
    }
}
