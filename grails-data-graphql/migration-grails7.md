# grails-data-graphql — Grails 6.1 → 7.1 migration notes

Target: **Grails 7.1.0** (Apache release, artifacts on Maven Central under `org.apache.grails:*:7.1.0`).
The module remains a **standalone build** (own `settings.gradle` / `gradle.properties`); it consumes Grails 7.1.0 as published dependencies, mirroring how it consumed Grails 6.

Verification after migration:

```
../gradlew build
```

- `:gorm-graphql:test` — 256 tests, 0 failures, 2 skipped (pre-existing `@Ignore`d MongoDB specs)
- `:grails-plugin-gorm-graphql-plugin:test` — 12 tests, 0 failures
- `:docs:asciidoctor` — reference guide built at `docs/build/docs/manual/index.html`
- `:examples-grails-test-app`, `:examples-grails-docs-app`, `:examples-grails-tenant-app`, `:examples-grails-multi-datastore-app`, `:examples-spring-boot-app` — all compile and `bootJar`/`assemble` cleanly

---

## 1. Stack upgrade

| Component | Before | After |
|---|---|---|
| Grails | 6.1.0 | **7.1.0** |
| Java | 11 | **17** |
| Groovy | 3.0.25 | **4.0.31** (group id: `org.codehaus.groovy` → `org.apache.groovy`) |
| Spring Boot | 2.7.x | **3.5.13** |
| Spring Framework | 5.3.x | 6.2.x (via Spring Boot 3.5) |
| Gradle | 7.x | **8.14.4** |
| Spock | 2.1-groovy-3.0 | **2.3-groovy-4.0** |
| graphql-java | 20.7 | **24.3** (aligned with Spring Boot 3.5 BOM) |
| Servlet API | `javax.servlet` 4.0.1 | **`jakarta.servlet` 6.0.0** |
| Hibernate | 5.6.11 | **5.6.15.Final** — Jakarta variant (`hibernate-core-jakarta`). Grails 7.1 still ships Hibernate 5; Hibernate 6 is *not* used. |
| Micronaut HTTP client | 3.10.3 | **4.9.9** |
| Micronaut RxJava2 client | 1.3.0 | **2.9.0** |
| Mock backend | `cglib-nodep` 3.3.0 (doesn't work on JDK 17+) | **`net.bytebuddy`** |

Versions are declared in [`gradle.properties`](gradle.properties). The `org.apache.grails:grails-bom:7.1.0` platform is imported in root [`build.gradle`](build.gradle) and manages most transitive versions.

## 2. Namespace rename — Jakarta EE

Only two source files touched `javax.*`:

- `core/src/main/groovy/org/grails/gorm/graphql/Schema.groovy`
- `plugin/src/main/groovy/org/grails/gorm/graphql/plugin/GrailsGraphQLConfiguration.groovy`

Both replaced `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct`. No `javax.persistence` / `javax.validation` / `HttpServletRequest` references existed, so no other source edits were needed.

`plugin/build.gradle` swapped `javax.servlet:javax.servlet-api` → `jakarta.servlet:jakarta.servlet-api`.

## 3. Groovy 4 strict generics in the DataFetcher hierarchy

Groovy 4 no longer allows a class to implement the same interface twice with different type arguments (raw + parameterized). This surfaced in `CountEntityDataFetcher extends DefaultGormDataFetcher<Integer> implements ReadingGormDataFetcher`, where `ReadingGormDataFetcher` extended raw `DataFetcher` while the parent bound `DataFetcher<Integer>`.

Fix — parameterize the interface chain and propagate to implementers:

- Interfaces now `<T>`: `GormDataFetcher<T>`, `ReadingGormDataFetcher<T>`, `BindingGormDataFetcher<T>`, `PaginatingGormDataFetcher<T>`.
- Trait now `<T>`: `DeletingGormDataFetcher<T>`.
- Implementers pass through the type parameter: `CountEntityDataFetcher`, `EntityDataFetcher`, `SingleEntityDataFetcher`, `PaginatedEntityDataFetcher`, `CreateEntityDataFetcher`, `UpdateEntityDataFetcher`, `DeleteEntityDataFetcher`.

Backward compatible at the bytecode / raw-type level for downstream consumers.

## 4. graphql-java 20.7 → 24.3

Spring Boot 3.5.13’s BOM pins graphql-java at 24.3. The old declared 20.7 was silently upgraded, which broke `MockDataFetchingEnvironment` because `graphql.cachecontrol.CacheControl` was removed in graphql-java 22.

Fix — in `core/src/main/groovy/org/grails/gorm/graphql/testing/MockDataFetchingEnvironment.groovy`: dropped the `CacheControl` import, field, and `getCacheControl()` override. `DataFetchingEnvironment` no longer declares that method in graphql-java 24.

## 5. Spring 6 / Grails 7 API adjustments

- `GraphqlController`: `HttpMethod.resolve(request.method)` → `HttpMethod.valueOf(request.method)` (Spring 6 removed `resolve`).
- `GraphqlController.browser()`: guarded against a missing `graphiql.html` classpath resource. Upstream commit `a9b9fa2598` removed the bundled GraphiQL assets; the action now returns 404 instead of NPE-ing. Downstream apps that ship their own `graphiql.html` still render it.
- `GrailsGraphQLConfiguration`: inlined the `DEFAULT_DATE_FORMATS` list locally. The published Grails 7.1.0 `DataBindingGrailsPlugin` no longer exposes this constant (it moved to `AbstractDataBindingGrailsPlugin`, which isn’t in the 7.1.0 artifact).
- `DefaultGormDataFetcher.queryInstance()`: added a `(GormEntity)` cast — newer GORM’s `DetachedCriteria.get(...)` returns `Object`, which Groovy 4’s stricter static type checker rejects for a `GormEntity`-typed return.

## 6. Plugin descriptor

`GormGraphqlGrailsPlugin.groovy` — `grailsVersion` widened from `"4.0.0 > *"` to `"7.1.0 > *"`. The Spring bean wiring in `doWithSpring()` is unchanged; bean names are preserved (consumers override by name).

## 7. Build plumbing

Root [`build.gradle`](build.gradle):

- Stopped applying `grails-common-build/common-project.gradle` and `common-publishing.gradle` (both rely on Gradle 7 APIs like `Jar.classifier` and declare the legacy `org.codehaus.groovy:groovy` coord). Replaced with a minimal inline subprojects config.
- Added a `resolutionStrategy` that redirects transitively-leaked `org.codehaus.groovy:*` → `org.apache.groovy:*` at the declared `groovyVersion`, and excludes the misdeclared `org.codehaus.groovy:groovy-bom` outright.
- Switched Java config from `sourceCompatibility = 1.11` to `sourceCompatibility = JavaVersion.toVersion(javaVersion)` (`javaVersion=17`). Toolchain-based config was avoided because it requires an exact JDK 17 install; source/target 17 lets any JDK ≥ 17 compile.
- Test config: `useJUnitPlatform()` applied globally to all `Test` tasks.

## 8. Restored upstream-deleted files

Commit `a58aa145cf` (“Integrate grails-data-mapping into grails-core”) accidentally dropped 7 files referenced by `DefaultGraphQLTypeManager`:

- `core/src/main/.../types/output/AbstractObjectTypeBuilder.groovy`
- `core/src/main/.../types/output/ObjectTypeBuilder.groovy`
- `core/src/main/.../types/output/EmbeddedObjectTypeBuilder.groovy`
- `core/src/main/.../types/output/PaginatedObjectTypeBuilder.groovy`
- `core/src/main/.../types/output/ShowObjectTypeBuilder.groovy`
- `core/src/test/.../types/output/EmbeddedObjectTypeBuilderSpec.groovy`
- `core/src/test/.../types/output/ShowObjectTypeBuilderSpec.groovy`

All restored from `a58aa145cf^` verbatim.

## 9. Test expectation updates (9 assertions)

Grails 7’s GORM no longer auto-populates `ConstrainedProperty.order` for properties without an explicit order, so `OrderedGraphQLProperty.compareTo` falls back to the natural `entity.persistentProperties` iteration order. The production path remains deterministic; only the test expectations baked in the GORM 6 order.

- `DefaultGraphQLDomainPropertyManagerSpec` — 5 assertions rewritten to the new property order.
- `HibernatePersistentGraphQLPropertySpec` — `orderNullc` expected `order 6` → `5`; `orderNulld` `7` → `6`.
- `EmbeddedInputObjectTypeBuilderSpec` — `['many','one']` → `['one','many']` (×2).

No production code behavior changed for these tests.

## 10. `docs` and `examples/*` — rewired to Grails 7.1.0

All previously-disabled subprojects are now re-enabled in [`settings.gradle`](settings.gradle) and compile cleanly under `./gradlew build`.

- **[`docs/build.gradle`](docs/build.gradle)** — dropped `grails-common-build/common-docs.gradle` (removed `org.grails:grails-docs` coord). Replaced with a direct `org.asciidoctor.jvm.convert:4.0.5` plugin applied via the `plugins { }` block; asciidoctorj 3.0.0. Composite [`docs/src/main/docs/index.adoc`](docs/src/main/docs/index.adoc) generated from `toc.yml` to replace the legacy per-topic pipeline.
- **`examples/grails-{test,docs,tenant,multi-datastore}-app/build.gradle`** — all four Grails example apps rewritten against the Grails 7.1.0 BOM: every dep resolved via `platform("org.apache.grails:grails-bom:$grailsBomVersion")` (no version pins inline). Replaced `project(':grails-testing-support-datamapping')` (monorepo-only) → `org.apache.grails:grails-testing-support-datamapping` (Maven Central). Dropped `org.grails:grails-test-mixins:3.3.0`, `selenium-htmlunit-driver:2.47.1`, `htmlunit:2.18`, `grails.plugins:embedded-mongodb:2.0.1`, `org.glassfish.web:el-impl:2.1.2-b03` — all dead coordinates or JDK-17-incompatible. `micronaut-rxjava2-http-client` 1.2.0 → 2.9.0. Hibernate dep switched to `hibernate-core-jakarta` (matching Grails 7.1.0 Jakarta variant).
- **`examples/grails-multi-datastore-app` integration specs** — `grails.test.mixin.integration.Integration` → `grails.testing.mixin.integration.Integration` (package moved in Grails 7).
- **`examples/spring-boot-app/build.gradle`** — rewritten as a truly standalone Spring Boot 3.5 demo: buildscript pulls `spring-boot-gradle-plugin:3.5.13` from Maven Central; `classpath platform(project(":grails-bom"))` replaced with `implementation platform("org.apache.grails:grails-bom:$grailsBomVersion")`; depends on published `org.apache.grails.data:grails-data-hibernate5-core` + `grails-datamapping-core` rather than monorepo-only project refs. The one remaining project dep is `project(':gorm-graphql')` (the local core library under test).
- **`examples/spring-boot-app` test suite** — JUnit 4 (`org.junit.Test` / `SpringRunner`) → JUnit 5 + Spock (`Specification` + `@SpringBootTest`). Two files rewritten.

## 11. Still deferred

- **`MongoSchemaSpec`** — fongo 2.1.1 is abandoned and incompatible with MongoDB driver 5.x shipped by Grails 7.1. The spec was already `@Ignore`d upstream; file renamed to `.groovy.disabled` and `fongo` / `grails-data-mongodb-core` test deps commented out. Follow-up: replace with Testcontainers Mongo.
- **Publishing** — `common-publishing.gradle` apply removed (Gradle-7-only `Jar.classifier` API). Rewire against the grails-7.1.0 publishing pipeline separately.

## 12. Dependency policy

`grails-data-graphql` is a community-maintained plugin, so the migration deliberately **keeps** the Micronaut HTTP clients and `grails-converters` as dependencies even though they could have been pruned. Versions were bumped rather than the deps removed, to preserve the transitive surface that downstream apps may rely on.
