package com.friendavailability.base;

import com.friendavailability.config.LinkUpTestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = LinkUpTestConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

}