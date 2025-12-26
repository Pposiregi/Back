package com.fitpet.server.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String DEV_USER_HEADER = "dev-user-id";

    @Bean
    public OpenAPI openAPI() {
       
        /* 2025.12.26 MANDARIN] 현재 Swegger 페이지의 origin 기준 API 호출 */
        return new OpenAPI()
            .components(
                new Components().addSecuritySchemes(
                    DEV_USER_HEADER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(DEV_USER_HEADER)
                        .description("개발용 사용자 ID (예: 3)")
                )
            )
          .addSecurityItem(new SecurityRequirement().addList(DEV_USER_HEADER))
          .servers(List.of(new Server().url("/")));
    }
}
