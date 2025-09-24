package com.friendavailability.infrastructure;

import com.friendavailability.base.BaseIntegrationTest;
import com.friendavailability.base.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInfrastructureVerificationTest extends BaseIntegrationTest{

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldLoadTestProfile(){
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }

    @Test
    void shouldUseH2Database() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            assertThat(databaseProductName).isEqualTo("H2");
        }
    }

    @Test
    void shouldUseMockMailSender() {
        assertThat(mailSender.getClass().getName()).contains("MockitoMock");
    }

    @Test
    void shouldHavePasswordEncoder() {
        String encoded = passwordEncoder.encode("testPassword");
        assertThat(passwordEncoder.matches("testPassword", encoded)).isTrue();
    }
}
