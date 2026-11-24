# Getting started with the Smart Warehouse API

This guide takes you from a fresh clone to your first API calls in about 10 minutes. Then the tutorial [Connect a shelf camera](tutorials/connect-shelf-camera.md) walks through a full integration.

| What | Where |
|---|---|
| API reference (Redoc) | **https://skri1l.github.io/Smart-Warehouse/api/** (built from `openapi.yaml`) |
| OpenAPI 3.1 specification | [`openapi.yaml`](openapi.yaml) |
| Interactive Swagger UI | `http://localhost:8080/swagger-ui.html` (while the backend runs locally) |
| How the spec is built and validated | [README.md](README.md) |

## 1. Prerequisites

- **Docker** with Docker Compose. This is the easiest option: it starts PostgreSQL, the Vision service and the backend together.
- Or, without Docker: **Java 21** and **PostgreSQL 15+** for the backend only (photo upload then needs the Vision service from [`Vision/`](../../Vision/README.md)).
- `curl`, or any HTTP client (Postman, Insomnia, HTTPie).

## 2. Start the backend

**With Docker (recommended):**

```bash
cd Backend
cp .env.example .env          # set POSTGRES_PASSWORD and JWT_SECRET; change ports if 8080/5432 are taken
docker compose up -d --build  # postgres + vision + backend
```

**Without Docker** (PostgreSQL already running, database `smart_warehouse`):

```bash
cd Backend
export SPRING_DATASOURCE_USERNAME=smart_warehouse SPRING_DATASOURCE_PASSWORD=<your password>
export JWT_SECRET=$(openssl rand -base64 32)   # signs access tokens; Base64, at least 32 bytes
./mvnw spring-boot:run
```

Liquibase creates the schema on the first start. The API listens on **`http://localhost:8080`** (or on `BACKEND_PORT` from `.env`).

Check that it is up:

```bash
curl -s http://localhost:8080/v3/api-docs | head -c 80
# {"openapi":"3.1.0","info":{"title":"Smart Warehouse API", ...
```

## 3. Get an access token

Every endpoint except `POST /auth/register` and `POST /auth/login` needs a JWT access token. Without it the backend answers `401` with an empty body.

**Register a user** (new users get the role `WORKER`):

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "anna@example.com", "username": "anna", "password": "S3cure-passw0rd"}'
# 1
```

**Sign in** and keep the token in a shell variable:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "anna", "password": "S3cure-passw0rd"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
```

The response is `{"accessToken": "eyJraWQiOi..."}`. The token is valid for **15 minutes**; when a call returns `401`, sign in again. All examples below send it as `Authorization: Bearer $TOKEN`.

In Swagger UI press **Authorize**, paste the token (without the word `Bearer`), and every **Try it out** request will carry it.

## 4. Your first calls

**Create a warehouse.** `POST` returns `201 Created` and only the **ID** of the new resource:

```bash
curl -i -X POST http://localhost:8080/warehouses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Central warehouse", "address": "Gedimino pr. 1, Vilnius"}'
```

```http
HTTP/1.1 201
Content-Type: application/json

1
```

**Add a product:**

```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku": "SKU-1001", "name": "Milk 1L", "description": "UHT milk 3.2%, 1 litre carton"}'
# 1
```

**Read it back.** `GET` returns the full object:

```bash
curl http://localhost:8080/products/1 -H "Authorization: Bearer $TOKEN"
```

```json
{ "id": 1, "sku": "SKU-1001", "name": "Milk 1L", "description": "UHT milk 3.2%, 1 litre carton" }
```

**List and search.** Lists are plain JSON arrays:

```bash
curl "http://localhost:8080/products/search?name=milk" -H "Authorization: Bearer $TOKEN"
# [{"id":1,"sku":"SKU-1001","name":"Milk 1L","description":"UHT milk 3.2%, 1 litre carton"}]
```

Prefer clicking? Open `http://localhost:8080/swagger-ui.html`, press **Authorize**, then expand an endpoint and press **Try it out**.

## 5. Conventions you need to know

| Topic | Rule |
|---|---|
| Base URL | `http://localhost:8080`. Paths start at the root, e.g. `/products`. A `/api/v1` prefix is planned ([API_GUIDELINES.md](../API_GUIDELINES.md)). |
| Authentication | `Authorization: Bearer <token>` from `POST /auth/login`, valid 15 minutes. Only `/auth/login` and `/auth/register` are public. |
| Roles | The token carries the role (`ROLE_WORKER`, `ROLE_MANAGER`, `ROLE_ADMIN`), but roles are not enforced yet: any signed-in user can call any endpoint. |
| Create | `POST` → `201` with the new ID as a bare number. |
| Update | `PUT` replaces the **whole** resource (send every field) → `200` with the updated object. |
| Delete / actions | `DELETE`, `PATCH …/activate`, `…/approve` etc. → `204 No Content`. |
| Dates | `2026-12-01` for dates; `2026-11-23T10:15:00` for date-times (server local time, no timezone). |
| Enums | Upper case: `ACTIVE`, `REMOVAL`, `CAMERA`, … (each schema in the reference lists the values). |

## 6. Handling errors

Check the HTTP status first, then show `message` to people. Do not parse `message` in code: its wording may change.

```bash
curl http://localhost:8080/products/999 -H "Authorization: Bearer $TOKEN"
```

```json
{ "status": 404, "message": "Product is not found" }
```

```bash
curl -X POST http://localhost:8080/products -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"sku": ""}'
```

```json
{
  "timestamp": "2026-11-23T20:38:10.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='productRequestDto'. Error count: 2",
  "path": "/products"
}
```

| Status | When | What to do |
|---|---|---|
| `400` | Invalid JSON, a required field missing or invalid (`@NotBlank`, `@Positive`, date in the past), wrong parameter type | Fix the request; nothing was changed |
| `401` | No token, an expired or malformed token, or wrong username/password on `/auth/login`. Empty body; the reason is in the `WWW-Authenticate` header | Sign in again and retry with the new token |
| `404` | The resource, or an ID you referenced (`productId`, `shelfId`…), does not exist | Check the IDs |
| `409` | Duplicate (SKU, device key, user e-mail, warehouse name + address) or a state conflict (inactive camera, out-of-order photo) | Read `message`; usually the data already exists |
| `415` | Wrong `Content-Type` (photo upload needs `multipart/form-data`) | Send the right content type |
| `502` | The Vision service is down or answered badly (photo upload only) | Retry later; the photo was not applied |
| `500` | Unexpected server error | Report it with the request |

Every endpoint in the [reference](https://skri1l.github.io/Smart-Warehouse/api/) lists its possible status codes with an example body.

## 7. Import into your tools

- **Postman / Insomnia:** *Import → File →* `Documentation/api/openapi.yaml`. You get a collection with all 115 operations and the `bearerAuth` scheme (set the token once on the collection) and example bodies.
- **Client code generation:** `npx @openapitools/openapi-generator-cli generate -i Documentation/api/openapi.yaml -g typescript-fetch -o client/` (or `-g java`, `-g python`…).

## Next step

Follow [Connect a shelf camera](tutorials/connect-shelf-camera.md): build a warehouse layout, register a camera, upload a photo and watch the stock change.
