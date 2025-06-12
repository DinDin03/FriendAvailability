package com.friendavailability;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FriendAvailabilityAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(FriendAvailabilityAppApplication.class, args);
		System.out.println("Friend Availability App is running!");
		System.out.println("Try visiting: http://localhost:8080");
	}
}