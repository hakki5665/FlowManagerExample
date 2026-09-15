package com.example.flowmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowmanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowmanagerApplication.class, args);
	}
}