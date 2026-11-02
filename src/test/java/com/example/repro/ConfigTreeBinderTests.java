package com.example.repro;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// Same problem without an application context: one Binder, one fresh ApplicationConversionService.
class ConfigTreeBinderTests {

	private final Binder binder = new Binder(
			List.of(ConfigurationPropertySource.from(new ConfigTreePropertySource("tree", Path.of("configtree")))), null,
			new ApplicationConversionService());

	// BUG: fails with ConverterNotFoundException (PropertyFileContent -> Integer)
	@Test
	void collectionThenScalar() {
		assertThat(this.binder.bind("collection.ids", Bindable.setOf(Integer.class)).get()).hasSize(3);
		assertThat(this.binder.bind("scalar.timeout", Integer.class).get()).isEqualTo(300000);
	}

	// Control: passes
	@Test
	void scalarThenCollection() {
		assertThat(this.binder.bind("scalar.timeout", Integer.class).get()).isEqualTo(300000);
		assertThat(this.binder.bind("collection.ids", Bindable.setOf(Integer.class)).get()).hasSize(3);
	}

}
