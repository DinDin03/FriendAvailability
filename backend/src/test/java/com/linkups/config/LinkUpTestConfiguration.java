package com.linkups.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Mockito;

@TestConfiguration
public class LinkUpTestConfiguration {

    @Bean
    @Primary
    public JavaMailSender mockMailSender(){
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
