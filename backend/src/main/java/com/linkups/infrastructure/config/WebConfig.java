package com.linkups.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.debug("Configuring static resource handlers for email templates");

        // Email templates and verification pages
        registry.addResourceHandler("/email/**")
                .addResourceLocations("classpath:/static/email/")
                .setCachePeriod(3600);

        log.debug("Static resource handlers configured successfully");
    }
}