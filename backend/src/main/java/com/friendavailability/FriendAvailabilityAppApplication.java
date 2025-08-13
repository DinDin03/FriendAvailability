package com.friendavailability;

import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FriendAvailabilityAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(FriendAvailabilityAppApplication.class, args);
		System.out.println("Friend Availability App is running!");
		System.out.println("Visit: http://localhost:8080");
	}
}