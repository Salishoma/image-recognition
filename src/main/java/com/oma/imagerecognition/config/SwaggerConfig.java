package com.oma.imagerecognition.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(apiInfo());

    }

    private Info apiInfo() {
        return new Info()
                .title("Image Recognition")
                .description("Image Recognition")
                .version("1.0")
                .contact(new Contact()
                        .name("Abubakar Salifu")
                        .email("mailto:abubakaroma91@gmail.com")
                        .url("https://github.com/salishoma")
                );
    }
}
