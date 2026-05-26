package com.mindtalk.forum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置
 */
@Configuration
public class Knife4jConfig {

    @Value("${knife4j.api-title:MindTalk 思享论坛 API}")
    private String apiTitle;

    @Value("${knife4j.api-description:MindTalk 论坛接口文档}")
    private String apiDescription;

    @Value("${knife4j.api-version:1.0.0}")
    private String apiVersion;

    @Value("${knife4j.contact-name:MindTalk Team}")
    private String contactName;

    @Value("${knife4j.license-name:Apache 2.0}")
    private String licenseName;

    @Value("${knife4j.license-url:https://www.apache.org/licenses/LICENSE-2.0}")
    private String licenseUrl;

    @Bean
    public OpenAPI mindtalkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(apiTitle)
                        .description(apiDescription)
                        .version(apiVersion)
                        .contact(new Contact()
                                .name(contactName))
                        .license(new License()
                                .name(licenseName)
                                .url(licenseUrl)))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
