package com.startin.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StartIn Backend API")
                        .version("1.0.0")
                        .description("API RESTful desenvolvida em Spring Boot (Java 21) integrada diretamente ao microserviço de Inteligência Artificial em Python para matchmaking de startups com editais, POCs, eventos e compras.")
                        .contact(new Contact()
                                .name("StartIn Dev Team")
                                .email("suporte@startin.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://spring.io")));
    }
}
