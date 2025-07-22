package com.friendavailability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("Configuring static resource handlers...");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600); 
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("classpath:/static/pages/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/email/**")
                .addResourceLocations("classpath:/static/email/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/images/**", "/assets/**", "/favicon.ico")
                .addResourceLocations("classpath:/static/images/", 
                                    "classpath:/static/assets/", 
                                    "classpath:/static/")
                .setCachePeriod(86400); 
        
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
                
        System.out.println("Static resource handlers configured successfully");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        System.out.println("Configuring view controllers...");
        
        registry.addViewController("/").setViewName("forward:/index.html");
        
        registry.addViewController("/dashboard").setViewName("forward:/pages/dashboard.html");
        
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/signup").setViewName("forward:/index.html");
        
        System.out.println("View controllers configured successfully");
    }
}