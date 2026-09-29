package pl.reachsky.backend.catalog.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.reachsky.backend.AbstractIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ListingSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings, user_profiles");
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String validBody() {
        return """
                {
                  "title": "Mountain cabin",
                  "description": "Great views",
                  "windowStart": "2027-09-01T14:00:00Z",
                  "windowEnd":   "2027-09-03T10:00:00Z",
                  "capacity": 2
                }
                """;
    }

    @Test
    void getListings_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk());
    }

    @Test
    void createListing_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListing_withUserRoleOnly_returns403() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .with(jwt()
                                .jwt(j -> j
                                        .subject(UUID.randomUUID().toString())
                                        .claim("preferred_username", "buyer")
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createListing_withSellerRole_returns201() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .with(jwt()
                                .jwt(j -> j
                                        .subject(UUID.randomUUID().toString())
                                        .claim("preferred_username", "seller")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "SELLER"))))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated());
    }
}
