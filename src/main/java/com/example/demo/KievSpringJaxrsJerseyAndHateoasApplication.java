package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class KievSpringJaxrsJerseyAndHateoasApplication {
	public static ObjectMapper mapperSpring;

	public static void main(String[] args) {
		SpringApplication.run(KievSpringJaxrsJerseyAndHateoasApplication.class, args);
	}

}
