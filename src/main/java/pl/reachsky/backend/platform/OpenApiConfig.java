package pl.reachsky.backend.platform;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ReachSky API")
                        .description("Aukcje zasobów nietrwałych w czasie")
                        .version("0.0.1-SNAPSHOT"));
    }

    @Bean
    OpenApiCustomizer namedSchemasCustomizer() {
        return openApi -> {
            // ListingStatus — wyciągnięty z inline żeby frontend mógł:
            //   export type ListingStatus = components['schemas']['ListingStatus']
            openApi.getComponents().addSchemas("ListingStatus",
                    new StringSchema()._enum(List.of("DRAFT", "ACTIVE", "CLOSED")));

            // ProblemDetail (RFC 9457) — wymagany przez frontend
            openApi.getComponents().addSchemas("ProblemDetail",
                    new Schema<>().type("object")
                            .addProperty("type", new StringSchema())
                            .addProperty("title", new StringSchema())
                            .addProperty("status", new Schema<Integer>().type("integer").format("int32"))
                            .addProperty("detail", new StringSchema())
                            .addProperty("instance", new StringSchema()));

            openApi.getComponents().addSchemas("AuctionType",
                    new StringSchema()._enum(List.of("ENGLISH", "DUTCH")));

            openApi.getComponents().addSchemas("AuctionStatus",
                    new StringSchema()._enum(List.of(
                            "DRAFT", "SCHEDULED", "RUNNING", "SOLD",
                            "RESERVE_NOT_MET", "CANCELLED", "SETTLED")));
        };
    }
}
