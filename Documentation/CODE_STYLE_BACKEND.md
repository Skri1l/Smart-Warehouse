# Backend Code Style (Java / Spring Boot)

Base style: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), mechanically enforced by **Spotless** (`google-java-format`) — see `Backend/pom.xml`. Do not hand-format code; run the formatter.

```bash
cd Backend
./mvnw spotless:apply    # auto-format before committing
./mvnw spotless:check    # fails if anything is unformatted (also runs on `verify`/CI)
```

## Package structure

Package root: `com.skr1l.backend`. Organize by **technical layer** at the top level and by **domain module** underneath, so each feature is easy to locate:

```text
com.skr1l.backend
├── config/                 Spring configuration (security, OpenAPI, CORS, beans)
├── common/                 Shared utilities, base classes, generic exceptions
├── <module>/               e.g. inventory, warehouse, product, auth, recognition
│   ├── controller/         REST controllers — HTTP concerns only
│   ├── service/            Business logic
│   ├── repository/         Spring Data JPA repositories
│   ├── entity/             JPA entities
│   ├── dto/                Request/response DTOs (never expose entities directly)
│   ├── mapper/             Entity <-> DTO mapping (MapStruct)
│   └── exception/          Module-specific exceptions
└── BackendApplication.java
```

## Naming

| Element | Convention | Example |
|---|---|---|
| Class / interface | `UpperCamelCase` | `StockMovementService` |
| Method / field / parameter | `lowerCamelCase` | `findByShelfId` |
| Constant (`static final`) | `UPPER_SNAKE_CASE` | `MAX_UPLOAD_SIZE_MB` |
| Package | `lowercase`, no underscores | `com.skr1l.backend.inventory` |
| REST controller | `<Resource>Controller` | `ProductController` |
| Service | `<Domain>Service` (+ `Impl` only if an interface is genuinely needed) | `ReplenishmentService` |
| Repository | `<Entity>Repository` | `ShelfStockRepository` |
| DTO | `<Purpose><Entity>Request` / `...Response` | `CreateProductRequest`, `ProductResponse` |
| Test class | `<ClassUnderTest>Test` (unit) / `...IT` (integration) | `ReplenishmentServiceTest`, `ProductControllerIT` |
| Test method | `should_<expectedBehavior>_when_<condition>` | `should_returnConflict_when_skuAlreadyExists` |

## Layering rules

- **Controllers** only: validate input (`@Valid`), call one service method, map the result to an HTTP response. No business logic in controllers.
- **Services** contain business logic and orchestrate repositories. Services depend on other services/repositories, never on controllers or DTOs from other modules.
- **Entities never leave the service layer.** Controllers and clients only ever see DTOs. Use MapStruct mappers for conversion, not manual field-by-field copying.
- Constructor injection only (`private final` fields) — no `@Autowired` on fields. Lombok's `@RequiredArgsConstructor` is preferred over hand-written constructors.

## Lombok usage

Allowed: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`, `@EqualsAndHashCode` (with explicit `of`/`exclude` on entities to avoid lazy-loading issues).

Avoid `@Data` on JPA entities (it generates `equals`/`hashCode`/`toString` over all fields, which is unsafe with lazy associations and mutable collections) — compose the specific annotations you need instead.

## Validation & error handling

- Validate DTOs with Bean Validation annotations (`@NotNull`, `@Size`, `@Positive`, …) at the controller boundary.
- All exceptions are translated to the common error response documented in [API_GUIDELINES.md](API_GUIDELINES.md) by a single `@RestControllerAdvice` (`common/exception/GlobalExceptionHandler`). Do not `try/catch` and format error JSON in individual controllers.
- Define domain exceptions per module (e.g. `ProductNotFoundException`) instead of throwing generic `RuntimeException`.

## Logging

- Use SLF4J via Lombok's `@Slf4j` (`log.info(...)`, `log.warn(...)`, `log.error(...)`). Never use `System.out.println` / `printStackTrace`.
- Do not log sensitive data (passwords, tokens, full request bodies containing personal data).
- Log at the boundary where the decision is made (e.g. log a rejected low-confidence recognition in the service that rejects it), not redundantly at every layer.

## Testing

- Unit tests: JUnit 5 + Mockito, one test class per production class, Arrange-Act-Assert structure, no Spring context unless necessary.
- Integration tests (`*IT`): load the Spring context, use Testcontainers for PostgreSQL instead of H2, so tests run against the real database engine.
- Test method names describe behavior, not implementation (see naming table above).
- Every new endpoint needs at least: one happy-path controller/integration test and one validation/error-path test.

## Documentation in code

- Public classes and non-trivial public methods in `service` and `controller` packages get a short Javadoc summary — what it does and any non-obvious constraint. Do not restate the method signature in prose.
- REST endpoints are documented for API consumers via OpenAPI annotations, not Javadoc — see [API_GUIDELINES.md](API_GUIDELINES.md).
- No commented-out code committed to `develop`/`main`. Delete it — git history keeps it if it's ever needed again.

## Formatting reference

Spotless/`google-java-format` handles indentation (2 spaces), line length, import order and braces automatically — these are not decisions individual contributors need to make. If `./mvnw spotless:check` passes, the formatting is correct.
