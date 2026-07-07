package com.travelease.backend;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

	private static final String BEARER_AUTH = "bearerAuth";

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public OpenAPI travelEaseOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("TravelEase API")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
								.name(BEARER_AUTH)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

}
