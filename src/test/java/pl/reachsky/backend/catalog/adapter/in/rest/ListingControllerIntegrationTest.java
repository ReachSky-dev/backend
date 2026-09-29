package pl.reachsky.backend.catalog.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import pl.reachsky.backend.AbstractIntegrationTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListingControllerIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    RestClient client;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings");
        client = RestClient.create("http://localhost:" + port);
    }

    private String validRequestBody() {
        return """
                {
                  "sellerId": "%s",
                  "title": "Cozy mountain cabin",
                  "description": "Great views",
                  "windowStart": "2027-08-10T14:00:00Z",
                  "windowEnd":   "2027-08-12T10:00:00Z",
                  "capacity": 4
                }
                """.formatted(UUID.randomUUID());
    }

    @Test
    void createListing_returns201_withDraftStatus() {
        ResponseEntity<ListingResponse> response = client.post()
                .uri("/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DRAFT");
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void publishListing_returns200_withActiveStatus() {
        ListingResponse created = client.post()
                .uri("/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        ResponseEntity<ListingResponse> published = client.post()
                .uri("/listings/{id}/publish", created.id())
                .retrieve()
                .toEntity(ListingResponse.class);

        assertThat(published.getStatusCode().value()).isEqualTo(200);
        assertThat(published.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void listActiveListings_returns200_withPublishedListings() {
        ListingResponse created = client.post()
                .uri("/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        client.post().uri("/listings/{id}/publish", created.id()).retrieve().toBodilessEntity();

        ListingResponse[] listings = client.get()
                .uri("/listings")
                .retrieve()
                .toEntity(ListingResponse[].class)
                .getBody();

        assertThat(listings).hasSize(1);
        assertThat(listings[0].status()).isEqualTo("ACTIVE");
    }

    @Test
    void publishListingTwice_returns409() {
        ListingResponse created = client.post()
                .uri("/listings")
                .header("Content-Type", "application/json")
                .body(validRequestBody())
                .retrieve()
                .toEntity(ListingResponse.class)
                .getBody();

        client.post().uri("/listings/{id}/publish", created.id()).retrieve().toBodilessEntity();

        ResponseEntity<String> secondPublish = client.post()
                .uri("/listings/{id}/publish", created.id())
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, res) -> {})
                .toEntity(String.class);

        assertThat(secondPublish.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
