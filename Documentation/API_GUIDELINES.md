# REST API Guidelines & Documentation Standard

These rules apply to every endpoint exposed by `Backend/`. They define both the wire format and how each endpoint must be documented so the OpenAPI spec is always accurate and the frontend can be built against it without reading Java source.

## Resource design

- Base path: `/api/v1/...` — the version prefix is mandatory from the first release; bump to `/api/v2` only on a breaking change, and support both during a deprecation window.
- Resources are plural nouns: `/api/v1/products`, `/api/v1/shelves/{shelfId}/stock`. No verbs in URLs (`/api/v1/products/search?query=...` is fine, `/api/v1/getProducts` is not).
- Nest resources only one level deep (`/shelves/{id}/stock`, not `/warehouses/{id}/racks/{id}/shelves/{id}/stock`); prefer a top-level filterable resource for anything deeper.
- Standard HTTP verbs/status codes:

| Verb | Use | Success status |
|---|---|---|
| `GET` | Read one/many, no side effects | `200` |
| `POST` | Create, or trigger an action that isn't idempotent (e.g. `/replenishment/recalculate`) | `201` (create) / `200` (action) |
| `PUT` | Full replace of a resource | `200` |
| `PATCH` | Partial update | `200` |
| `DELETE` | Remove | `204` |

## Pagination & filtering

- List endpoints are paginated by default: `?page=0&size=20` (0-indexed), response wraps results:

```json
{
  "content": [ /* items */ ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

- Filtering uses query parameters named after the field (`?category=beverages`), not a generic `filter=` blob.

## Error format

Every non-2xx response uses this shape (already defined in [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md#11-error-handling)):

```json
{
  "timestamp": "2026-10-01T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid request",
  "path": "/api/v1/products",
  "details": []
}
```

- `error` is a stable machine-readable code (`VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`, `RECOGNITION_LOW_CONFIDENCE`, …) — frontend code may switch on it. `message` is human-readable and may change wording; do not parse it.
- Produced in one place: `common/exception/GlobalExceptionHandler` (see [CODE_STYLE_BACKEND.md](CODE_STYLE_BACKEND.md)). Do not build this shape by hand in a controller.

## Authentication

- Endpoints (other than `/api/v1/auth/login` and device-ingestion endpoints) require a `Bearer <token>` `Authorization` header.
- Document per-endpoint role requirements (`WORKER`, `MANAGER`, `DEVICE`) with `@PreAuthorize`/`@SecurityRequirement` so they show up in Swagger UI.

## OpenAPI documentation standard

The spec is generated from code via **springdoc-openapi**, not written by hand. Every controller must be annotated so `GET /swagger-ui.html` and `GET /v3/api-docs` are always in sync with the implementation.

### Dependency (add when the first controller is implemented)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### Required annotations

- `@Tag(name = "Products")` on the controller class — groups endpoints in Swagger UI.
- `@Operation(summary = "...", description = "...")` on every endpoint method — one sentence on what it does, a longer description only if there's a non-obvious rule (e.g. "returns 409 if SKU already exists").
- `@ApiResponse(responseCode = "...", description = "...")` for every status code the endpoint can realistically return, including error cases.
- `@Schema(description = "...")` on DTO fields whose meaning isn't obvious from the name (units, valid ranges, enum meaning) — not on every field.
- `@Parameter(description = "...")` on path/query parameters that aren't self-explanatory.

### Example

```java
@Tag(name = "Products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @Operation(summary = "Create a product")
  @ApiResponse(responseCode = "201", description = "Product created")
  @ApiResponse(responseCode = "409", description = "SKU already exists")
  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    ProductResponse created = productService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
```

### DTO example

```java
public record ProductResponse(
    Long id,
    String sku,
    String name,
    @Schema(description = "Shelf life in days from production date, used by replenishment calculation")
        Integer shelfLifeDays) {}
```

## Versioning & breaking changes

- Additive changes (new optional field, new endpoint) do not require a version bump.
- Breaking changes (removing/renaming a field, changing a type, changing status codes) require a new `/api/v2` path and a `BREAKING CHANGE:` footer in the commit (see [COMMIT_CONVENTION.md](COMMIT_CONVENTION.md)). Keep the previous version running until the frontend has migrated.

## Frontend consumption

The PySide6 desktop client must not hand-write request/response shapes from memory — hand-maintain a typed client (`app/api/client.py`, see [CODE_STYLE_FRONTEND.md](CODE_STYLE_FRONTEND.md)) against the published `/v3/api-docs` schema, and re-check it whenever the backend OpenAPI spec changes. All network calls happen off the Qt UI thread (see CODE_STYLE_FRONTEND.md's threading rule) so a slow API response never freezes the window.
