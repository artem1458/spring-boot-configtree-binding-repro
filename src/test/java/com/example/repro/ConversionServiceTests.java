package com.example.repro;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

// Smallest form: merely asking canConvert(PropertyFileContent -> Set<Integer>) breaks PropertyFileContent -> Integer.
class ConversionServiceTests {

	@Test
	void canConvertToSetOfIntegerShouldNotAffectInteger() {
		Object value = new ConfigTreePropertySource("tree", Path.of("configtree")).getProperty("scalar.timeout");
		TypeDescriptor source = TypeDescriptor.forObject(value); // ConfigTreePropertySource$PropertyFileContent
		TypeDescriptor setOfInteger = new TypeDescriptor(ResolvableType.forClassWithGenerics(Set.class, Integer.class),
				null, null);
		ApplicationConversionService conversionService = new ApplicationConversionService();

		assertThat(conversionService.canConvert(source, setOfInteger)).isTrue();
		assertThat(conversionService.canConvert(source, TypeDescriptor.valueOf(Integer.class))).isTrue(); // fails
	}

}
