package pl.reachsky.backend.auction.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.shared.CurrentUser;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.UserId;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuctionControllerIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher(request -> true)
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @MockitoBean
    CurrentUserProvider currentUserProvider;

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    RestClient client;
    UUID sellerId;
    UUID listingId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE auctions");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        sellerId = UUID.randomUUID();
        listingId = insertListing(sellerId);

        client = RestClient.create("http://localhost:" + port);
        when(currentUserProvider.get()).thenReturn(
                new CurrentUser(new UserId(sellerId), "test-seller", Set.of("SELLER", "USER")));
    }

    @Test
    void createAuction_returns201_withScheduledStatus() {
        ResponseEntity<AuctionResponse> response = client.post()
                .uri("/api/auctions")
                .header("Content-Type", "application/json")
                .body(dutchRequest())
                .retrieve()
                .toEntity(AuctionResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(pl.reachsky.backend.auction.domain.AuctionStatus.SCHEDULED);
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void getAuction_returns200() {
        AuctionResponse created = client.post()
                .uri("/api/auctions")
                .header("Content-Type", "application/json")
                .body(dutchRequest())
                .retrieve()
                .toEntity(AuctionResponse.class)
                .getBody();

        ResponseEntity<AuctionResponse> found = client.get()
                .uri("/api/auctions/{id}", created.id())
                .retrieve()
                .toEntity(AuctionResponse.class);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().id()).isEqualTo(created.id());
    }

    @Test
    void cancelAuction_returns200_withCancelledStatus() {
        AuctionResponse created = client.post()
                .uri("/api/auctions")
                .header("Content-Type", "application/json")
                .body(dutchRequest())
                .retrieve()
                .toEntity(AuctionResponse.class)
                .getBody();

        ResponseEntity<AuctionResponse> cancelled = client.post()
                .uri("/api/auctions/{id}/cancel", created.id())
                .retrieve()
                .toEntity(AuctionResponse.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().status()).isEqualTo(pl.reachsky.backend.auction.domain.AuctionStatus.CANCELLED);
    }

    @Test
    void createAuction_withUnknownListing_returns404() {
        String unknownListingRequest = dutchRequest().replace(listingId.toString(), UUID.randomUUID().toString());

        ResponseEntity<String> response = client.post()
                .uri("/api/auctions")
                .header("Content-Type", "application/json")
                .body(unknownListingRequest)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void auctionListingId_isAccessibleViaGetListingEndpoint() {
        // Regression: GET /api/listings/{id} must exist and return the listing
        // regardless of status — so auction detail pages can show listing info.
        ResponseEntity<String> response = client.get()
                .uri("/api/listings/{id}", listingId)
                .retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listRunning_returns200_withEmptyListInitially() {
        ResponseEntity<AuctionResponse[]> response = client.get()
                .uri("/api/auctions")
                .retrieve()
                .toEntity(AuctionResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    private UUID insertListing(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '1 day', 1, 'ACTIVE', now())
                """, id, sellerId);
        return id;
    }

    private String dutchRequest() {
        return """
                {
                  "listingId": "%s",
                  "type": "DUTCH",
                  "startsAt": "2028-01-01T12:00:00Z",
                  "endsAt":   "2028-01-02T12:00:00Z",
                  "reservePriceAmount": 3000,
                  "currency": "PLN",
                  "startPriceAmount": 10000,
                  "decrementAmount": 1000,
                  "stepSeconds": 3600,
                  "floorAmount": 3000
                }
                """.formatted(listingId);
    }
}
