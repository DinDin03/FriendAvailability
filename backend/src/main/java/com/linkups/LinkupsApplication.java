package com.linkups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LinkupsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkupsApplication.class, args);
		System.out.println("Linkups is running!");
		System.out.println("Visit: http://localhost:5173");
	}
}