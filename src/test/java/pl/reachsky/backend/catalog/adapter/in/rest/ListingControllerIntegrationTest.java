package pl.reachsky.backend.catalog.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.catalog.domain.ListingStatus;
import pl.reachsky.backend.shared.CurrentUser;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.UserId;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListingControllerIntegrationTest extends AbstractIntegrationTest {

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

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");
        client = RestClient.create("http://localhost:" + port);
        when(currentUserProvider.get()).thenReturn(
                new CurrentUser(new UserId(UUID.randomUUID()), "test-seller", Set.of("SELLER", "USER")));
    }

    private String validRequestBody() {
        return """
                {
                  "title": "Cozy mountain cabin",
                  "description": "Great views",
                  "windowStart": "2027-08-10T14:00:00Z",
                  "windowEnd":   "2027-08-12T10:00:00Z",
                  "capacity": 4
                }
                """;
    }

    @Test
    void createListing_returns201_withDraftStatus() {
        ResponseEntity<ListingResponse> response = client.post()
                .uri("/api/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ListingStatus.DRAFT);
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void publishListing_returns200_withActiveStatus() {
        ListingResponse created = client.post()
                .uri("/api/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        ResponseEntity<ListingResponse> published = client.post()
                .uri("/api/listings/{id}/publish", created.id())
                .retrieve()
                .toEntity(ListingResponse.class);

        assertThat(published.getStatusCode().value()).isEqualTo(200);
        assertThat(published.getBody().status()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void listActiveListings_returns200_withPublishedListings() {
        ListingResponse created = client.post()
                .uri("/api/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        client.post().uri("/api/listings/{id}/publish", created.id()).retrieve().toBodilessEntity();

        ListingResponse[] listings = client.get()
                .uri("/api/listings")
                .retrieve()
                .toEntity(ListingResponse[].class)
                .getBody();

        assertThat(listings).hasSize(1);
        assertThat(listings[0].status()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void publishListingTwice_returns409() {
        ListingResponse created = client.post()
                .uri("/api/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        client.post().uri("/api/listings/{id}/publish", created.id()).retrieve().toBodilessEntity();

        ResponseEntity<String> secondPublish = client.post()
                .uri("/api/listings/{id}/publish", created.id())
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, res) -> {})
                .toEntity(String.class);

        assertThat(secondPublish.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
