package com.example.repro;

import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTreeBindingTests {

	// BUG: values from configtree:configtree/, Set<Integer> bound before Integer -> context fails to start
	@Test
	void configTreeCollectionBoundFirst() {
		try (ConfigurableApplicationContext context = run(CollectionFirst.class)) {
			assertBound(context);
		}
	}

	// Control: same config tree, Integer bound before Set<Integer> -> starts fine
	@Test
	void configTreeScalarBoundFirst() {
		try (ConfigurableApplicationContext context = run(ScalarFirst.class)) {
			assertBound(context);
		}
	}

	// Control: same values as plain String properties (no config tree), Set<Integer> bound first -> starts fine
	@Test
	void stringSourceCollectionBoundFirst() {
		try (ConfigurableApplicationContext context = run(CollectionFirst.class, "--spring.config.name=string-source")) {
			assertBound(context);
		}
	}

	private static ConfigurableApplicationContext run(Class<?> source, String... args) {
		return new SpringApplicationBuilder(source).run(args);
	}

	private static void assertBound(ConfigurableApplicationContext context) {
		assertThat(context.getBean(CollectionProperties.class).ids()).containsExactlyInAnyOrder(254, 169, 1413);
		assertThat(context.getBean(ScalarProperties.class).timeout()).isEqualTo(300000);
	}

	// Not @Configuration on purpose, so the component scan of ReproApplication never picks these up.
	// Binding order = @Bean method parameter order.
	@EnableConfigurationProperties({ CollectionProperties.class, ScalarProperties.class })
	static class CollectionFirst {

		@Bean
		Object consumer(CollectionProperties collection, ScalarProperties scalar) {
			return new Object();
		}

	}

	@EnableConfigurationProperties({ CollectionProperties.class, ScalarProperties.class })
	static class ScalarFirst {

		@Bean
		Object consumer(ScalarProperties scalar, CollectionProperties collection) {
			return new Object();
		}

	}

}
