# configtree: binding a collection first breaks a later binding of the same element type

Two `@ConfigurationProperties` records read their values from a config tree
(`spring.config.import=configtree:configtree/`):

| file                       | content         | bound to                                       |
|----------------------------|-----------------|------------------------------------------------|
| `configtree/collection.ids` | `254,169,1413` | `CollectionProperties(Set<Integer> ids)`       |
| `configtree/scalar.timeout` | `300000`       | `ScalarProperties(Integer timeout)`            |

If `CollectionProperties` is bound before `ScalarProperties`, the application fails to start.
The application binds them in that order on purpose: the `@Bean` method in `ReproApplication` takes them as parameters in that order.

## Run

```
./mvnw spring-boot:run          # run from the project root (the configtree/ path is relative)
./mvnw test                     # 106 tests: 60 fail (the bug), 46 pass (controls and String)
./mvnw test -Dspring-boot.version=3.5.16   # any other Boot version
```

## Expected

The application starts and prints
`Bound CollectionProperties[ids=[254, 169, 1413]] and ScalarProperties[timeout=300000]`.

## Actual

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to bind properties under 'scalar.timeout' to java.lang.Integer:

    Property: scalar.timeout
    Value: "300000"
    Origin: file [.../configtree/scalar.timeout] - 1:1
    Reason: org.springframework.core.convert.ConverterNotFoundException: No converter found capable of converting from type [org.springframework.boot.env.ConfigTreePropertySource$PropertyFileContent] to type [java.lang.Integer]
```

## Tests

- `ConfigTreeBindingTests`: full `SpringApplication` runs.
  - `configTreeCollectionBoundFirst`: fails (the bug).
  - `configTreeScalarBoundFirst`: the same config tree with the order reversed. Passes.
  - `stringSourceCollectionBoundFirst`: the same values as plain `String` properties. Passes.
- `ConfigTreeBinderTests`: the same failure with a bare `Binder` and a fresh `ApplicationConversionService`, so no application context is involved.
- `ElementTypeTests`: the same scenario for 20 element types, each as `Set<T>`, `List<T>` and `T[]`, with a fresh `Binder` per case.
  - A collection of T then a single T, from a config tree: fails for `Integer`, `Long`, `Short`, `Byte`, `Double`, `Float`, `Boolean`, `Character`, `BigDecimal`, `BigInteger`, `UUID`, `LocalDate`, `Duration`, `Period`, `DataSize`, an enum, `Locale`, `Charset` and `URI` (57 cases). Passes for `String`.
  - Controls: the single value bound first, and the same values as plain `String` properties. Both pass for every type.
  - Not covered by a test: a primitive target (`int` after `Set<Integer>`) also binds fine.
- `ConversionServiceTests`: the smallest form. After `canConvert(PropertyFileContent -> Set<Integer>)`, `canConvert(PropertyFileContent -> Integer)` returns `false` on the same `ApplicationConversionService`.

The order in `@EnableConfigurationProperties({A.class, B.class})` cannot be used to control the binding order: `EnableConfigurationPropertiesRegistrar` collects the types into a `Set`.

---

_AI disclosure: this sample and README were prepared with an AI agent (Claude Code) and reviewed by me._
