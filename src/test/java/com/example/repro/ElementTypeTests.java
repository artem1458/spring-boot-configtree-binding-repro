package com.example.repro;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThatNoException;

// The same scenario for many element types: a collection of T is bound from a config tree,
// then a single T from the same config tree. Each case uses a fresh Binder and a fresh
// ApplicationConversionService.
class ElementTypeTests {

	@TempDir
	Path tree;

	static Stream<ElementType> types() {
		return Stream.of(new ElementType(Integer.class, "1,2", "3"), new ElementType(Long.class, "1,2", "3"),
				new ElementType(Short.class, "1,2", "3"), new ElementType(Byte.class, "1,2", "3"),
				new ElementType(Double.class, "1.5,2.5", "3.5"), new ElementType(Float.class, "1.5,2.5", "3.5"),
				new ElementType(Boolean.class, "true,false", "true"), new ElementType(Character.class, "a,b", "c"),
				new ElementType(BigDecimal.class, "1.5,2.5", "3.5"), new ElementType(BigInteger.class, "1,2", "3"),
				new ElementType(UUID.class, "00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000002",
						"00000000-0000-0000-0000-000000000003"),
				new ElementType(LocalDate.class, "2025-01-01,2025-02-01", "2025-03-01"),
				new ElementType(Duration.class, "1s,2s", "3s"), new ElementType(Period.class, "P1D,P2D", "P3D"),
				new ElementType(DataSize.class, "1KB,2KB", "3KB"),
				new ElementType(TimeUnit.class, "SECONDS,MINUTES", "HOURS"),
				new ElementType(Locale.class, "en,fr", "de"),
				new ElementType(Charset.class, "UTF-8,US-ASCII", "UTF-8"),
				new ElementType(URI.class, "http://a,http://b", "http://c"),
				new ElementType(String.class, "a,b", "c"));
	}

	static Stream<Arguments> typesAndCollections() {
		return types().flatMap((type) -> Stream.of(Arguments.of(type, "Set", type.set()),
				Arguments.of(type, "List", type.list()), Arguments.of(type, "array", type.array())));
	}

	// BUG: fails with ConverterNotFoundException (PropertyFileContent -> T) for every type except String
	@ParameterizedTest(name = "{1}<{0}> then {0}")
	@MethodSource("typesAndCollections")
	void configTreeCollectionThenSingleValue(ElementType type, String collection, Bindable<?> many)
			throws IOException {
		Binder binder = configTreeBinder(type);
		assertThatNoException().isThrownBy(() -> {
			binder.bind("many", many).get();
			binder.bind("one", type.type()).get();
		});
	}

	// Control: the same config tree, the single value is bound first. Passes.
	@ParameterizedTest(name = "{0} then Set<{0}>")
	@MethodSource("types")
	void configTreeSingleValueThenCollection(ElementType type) throws IOException {
		Binder binder = configTreeBinder(type);
		assertThatNoException().isThrownBy(() -> {
			binder.bind("one", type.type()).get();
			binder.bind("many", type.set()).get();
		});
	}

	// Control: the same values as plain String properties, the collection is bound first. Passes.
	@ParameterizedTest(name = "Set<{0}> then {0}, String values")
	@MethodSource("types")
	void stringValuesCollectionThenSingleValue(ElementType type) {
		Binder binder = binder(new MapPropertySource("strings", Map.of("many", type.many(), "one", type.one())));
		assertThatNoException().isThrownBy(() -> {
			binder.bind("many", type.set()).get();
			binder.bind("one", type.type()).get();
		});
	}

	private Binder configTreeBinder(ElementType type) throws IOException {
		Path dir = Files.createTempDirectory(this.tree, type.toString());
		Files.writeString(dir.resolve("many"), type.many());
		Files.writeString(dir.resolve("one"), type.one());
		return binder(new ConfigTreePropertySource("tree", dir));
	}

	private static Binder binder(org.springframework.core.env.PropertySource<?> source) {
		return new Binder(List.of(ConfigurationPropertySource.from(source)), null, new ApplicationConversionService());
	}

	record ElementType(Class<?> type, String many, String one) {

		Bindable<?> set() {
			return Bindable.setOf(this.type);
		}

		Bindable<?> list() {
			return Bindable.listOf(this.type);
		}

		Bindable<?> array() {
			return Bindable.of(Array.newInstance(this.type, 0).getClass());
		}

		@Override
		public String toString() {
			return this.type.getSimpleName();
		}

	}

}
