package com.example.repro;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({ CollectionProperties.class, ScalarProperties.class })
public class ReproApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReproApplication.class, args);
	}

	// Binding order = parameter order: CollectionProperties is bound before ScalarProperties.
	// (The order in @EnableConfigurationProperties is NOT reliable: the registrar collects it into a Set.)
	@Bean
	ApplicationRunner printer(CollectionProperties collection, ScalarProperties scalar) {
		return (args) -> System.out.println("Bound " + collection + " and " + scalar);
	}

}
