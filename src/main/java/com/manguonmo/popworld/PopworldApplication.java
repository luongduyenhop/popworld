package com.manguonmo.popworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PopworldApplication {

	public static void main(String[] args) {
		SpringApplication.run(PopworldApplication.class, args);
	}

}
