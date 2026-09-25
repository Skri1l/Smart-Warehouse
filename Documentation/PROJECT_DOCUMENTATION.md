# Smart Warehouse — Project Documentation

## 1. Project overview

**Project name:** Smart Warehouse: An Intelligent Resource Storage System  
**Type:** diploma project / web + backend + computer vision + forecasting system  
**Team size:** 2 developers  
**Planned duration:** 12 weeks

Smart Warehouse is an intelligent warehouse management system designed for shelves equipped with cameras and a controller board. Each shelf has a unique identifier. When goods are added to or removed from a shelf, the camera captures an image and sends it to the server. The server uses computer vision to identify the product and estimate the quantity change. The result is stored in a relational database, so the system maintains near-real-time stock information.

The system also stores stock history, product shelf-life information and planned future demand. A forecasting and replenishment module determines whether the current stock is sufficient for an upcoming date. If not, it creates a replenishment recommendation and, in the extended version of the project, can automatically create an order through an external supplier integration.

A web application allows warehouse staff to see current stock, locate products, receive placement recommendations, inspect stock changes and review replenishment recommendations.

---

## 2. Project goals

The main goal is to reduce manual warehouse inventory operations and improve stock accuracy by combining shelf cameras, computer vision, a centralized backend and demand forecasting.

### Main objectives

- automatically detect product quantity changes on shelves;
- maintain current stock information in the database;
- keep a history of warehouse operations;
- show where every product is located;
- recommend where incoming goods should be placed;
- forecast future product demand;
- take product shelf life into account when calculating replenishment;
- generate replenishment recommendations;
- provide a responsive web interface for warehouse workers and managers;
- expose documented REST APIs for frontend and device integration;
- deploy the system to a cloud environment.

---

## 3. Scope

### Included in the diploma MVP

1. Warehouse, shelf and product management.
2. Unique identification of shelves.
3. Receiving images from shelf cameras.
4. Image processing pipeline.
5. Recognition of a configured set of product types.
6. Determining stock changes from image analysis.
7. Storing current stock and stock history.
8. Manual correction of recognition results.
9. Product location search.
10. Placement recommendations for incoming stock.
11. Product shelf-life data.
12. Planned-demand management.
13. Demand/replenishment calculation.
14. Replenishment recommendations.
15. Role-based web interface.
16. REST API documentation using OpenAPI/Swagger.
17. Cloud deployment.

### Optional / extended scope

- real supplier API integration;
- fully automatic purchasing without human confirmation;
- multiple warehouses;
- notifications by email/Telegram/Slack;
- advanced anomaly detection;
- support for many camera models;
- production-grade ML training pipeline.

Automatic supplier ordering should be treated as an extension unless a stable supplier API is available. For the diploma MVP, the system can create an internal purchase request or recommendation that a manager confirms.

---

## 4. Actors and roles

### Warehouse worker

- sees shelf map and current stock;
- searches for a product;
- sees where the product is stored;
- receives a recommendation for product placement;
- can confirm or report an incorrect recognition result.

### Warehouse manager / administrator

- manages products, shelves and users;
- sees warehouse stock and history;
- configures minimum stock and shelf-life parameters;
- creates planned demand;
- reviews replenishment recommendations;
- confirms purchase requests;
- reviews system events and recognition errors.

### Shelf device

- identifies itself by shelf/device ID;
- detects a shelf event or captures an image on request;
- sends an image and metadata to the backend.

### ML service

- receives an image;
- identifies the product class(es);
- estimates quantity or quantity change;
- returns result and confidence score.

---

## 5. Functional requirements

### FR-01 Authentication and authorization

The system shall support authenticated users and role-based access control.

### FR-02 Warehouse management

The administrator shall be able to create and edit warehouses, zones, racks and shelves.

### FR-03 Product management

The administrator shall be able to create products with SKU, name, category, unit, shelf life and optional recognition metadata.

### FR-04 Shelf-device registration

Every camera/controller device shall be linked to one shelf and have a unique identifier.

### FR-05 Image upload

A shelf device shall be able to upload an image together with shelf ID, device ID and capture timestamp.

### FR-06 Recognition

The backend shall pass the uploaded image to the ML service and receive detected product data, quantity/quantity delta and confidence.

### FR-07 Stock update

After a successful recognition result, the backend shall update the current stock balance and create a stock movement record.

### FR-08 Recognition validation

Low-confidence or invalid recognition results shall be marked for manual review instead of silently changing stock.

### FR-09 Stock history

The system shall preserve a history of stock changes including time, product, shelf, change amount and source.

### FR-10 Product location

A user shall be able to find all shelves containing a selected product.

### FR-11 Placement recommendation

For incoming goods, the system shall recommend an appropriate shelf based on configured capacity and current placement.

### FR-12 Planned demand

Managers shall be able to define future demand for a product for a given date and quantity.

### FR-13 Shelf-life awareness

The replenishment calculation shall take product shelf life into account so the system does not order stock too early when it would expire before use.

### FR-14 Demand forecast / replenishment

The system shall estimate future required quantity using stock history and/or configured future demand and compare it with available usable stock.

### FR-15 Purchase recommendation

When projected stock is insufficient, the system shall create a recommendation containing product, required quantity, required-by date and explanation.

### FR-16 Purchase confirmation

A manager shall be able to confirm, reject or modify a purchase recommendation.

### FR-17 Dashboard

The web application shall show current stock, low-stock products, recent movements, recognition errors and purchase recommendations.

### FR-18 API documentation

The REST API shall be documented with OpenAPI/Swagger.

---

## 6. Non-functional requirements

### NFR-01 Performance

Normal API operations should respond within approximately 2 seconds under diploma/demo load. Image recognition may take longer and should be processed asynchronously if necessary.

### NFR-02 Reliability

An image-processing failure shall not corrupt current stock. Failed events must be logged and retryable.

### NFR-03 Security

- authenticated access for business endpoints;
- role-based authorization;
- password hashing;
- validation of device requests;
- input validation;
- secrets stored outside source control.

### NFR-04 Auditability

Every automatic stock change shall be traceable to an event/image/recognition result.

### NFR-05 Scalability

The architecture should allow adding more shelves and devices without changing the business model.

### NFR-06 Maintainability

Backend, frontend and ML responsibilities should be clearly separated and API contracts documented.

### NFR-07 Responsiveness

The UI shall support desktop and mobile/tablet layouts.

---

## 7. Proposed architecture

```text
Shelf Camera + Controller
          |
          | image + shelf/device metadata
          v
      Backend API
          |
          +--------------------+
          |                    |
          v                    v
    ML / CV Service        Relational DB
          |                    |
          | result             | products, shelves,
          +-------> Backend <--+ stock, history,
                     |          plans, recommendations
                     |
          +----------+-----------+
          |                      |
          v                      v
       Frontend          Scheduler / Forecasting
                                |
                                v
                      Replenishment Recommendation
                                |
                                v
                       Purchase confirmation
```

### Recommended backend split

- Authentication / Users
- Warehouse structure
- Products
- Inventory
- Image events
- Recognition results
- Stock movements
- Demand plans
- Forecasting / replenishment
- Purchase recommendations
- Notifications / audit

### Recommended technology stack

**Backend:** Java, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Flyway/Liquibase, OpenAPI/Swagger.  
**Frontend:** React or another SPA framework selected by the frontend developer.  
**ML:** Python service with REST API; OpenCV + selected detection/counting model.  
**Infrastructure:** Docker / Docker Compose for local development, cloud deployment for demo.  
**Testing:** JUnit, Mockito, integration tests with PostgreSQL/Testcontainers; frontend tests where appropriate.

---

## 8. Main domain model

### User

- id
- email / username
- passwordHash
- role
- status

### Warehouse

- id
- name
- description

### Rack

- id
- warehouseId
- label

### Shelf

- id
- rackId
- label
- capacity
- status

### Device

- id
- shelfId
- deviceKey
- status
- lastSeenAt

### Product

- id
- sku
- name
- category
- unit
- shelfLifeDays
- minimumStock

### ShelfStock

- id
- shelfId
- productId
- quantity
- updatedAt

### ImageEvent

- id
- shelfId
- deviceId
- imagePath
- capturedAt
- receivedAt
- processingStatus

### RecognitionResult

- id
- imageEventId
- productId
- detectedQuantity / quantityDelta
- confidence
- status

### StockMovement

- id
- productId
- shelfId
- quantityDelta
- type
- source
- createdAt

### DemandPlan

- id
- productId
- requiredDate
- requiredQuantity

### ReplenishmentRecommendation

- id
- productId
- recommendedQuantity
- requiredBy
- reason
- status
- createdAt

### PurchaseOrder / PurchaseRequest

- id
- recommendationId
- productId
- quantity
- status
- createdAt

---

## 9. Key business flows

### Flow A — product removed from shelf

1. A shelf event occurs.
2. The camera captures an image.
3. The controller sends the image with shelf metadata to the backend.
4. Backend creates an `ImageEvent`.
5. ML service processes the image.
6. Recognition result is returned with confidence.
7. If confidence is sufficient, backend creates a negative `StockMovement` and updates current stock.
8. If confidence is low, event is sent to manual review.
9. The frontend shows the updated stock.

### Flow B — incoming goods placement

1. Worker selects/scans the incoming product.
2. Backend checks existing product locations and free shelf capacity.
3. System returns recommended shelf.
4. Worker places the product.
5. Camera event confirms the stock increase or worker confirms it manually.

### Flow C — replenishment calculation

1. Scheduler runs periodically.
2. System loads current stock, planned demand and recent consumption history.
3. It calculates stock expected to remain by the required date.
4. Product shelf life is considered when deciding when the product may be ordered.
5. If usable stock is insufficient, the system creates a replenishment recommendation.
6. Manager reviews and confirms/rejects it.
7. Extended version: confirmed recommendation is sent to an external supplier API.

---

## 10. API outline

Suggested endpoint groups:

```text
POST   /api/auth/login
GET    /api/users

GET    /api/warehouses
POST   /api/warehouses
GET    /api/shelves
POST   /api/shelves
GET    /api/shelves/{id}/stock

GET    /api/products
POST   /api/products
GET    /api/products/{id}/locations

POST   /api/device-events/images
GET    /api/image-events/{id}
GET    /api/recognition-results/review
PATCH  /api/recognition-results/{id}/confirm

GET    /api/inventory
GET    /api/inventory/movements
POST   /api/inventory/adjustments

GET    /api/placement/recommendations

GET    /api/demand-plans
POST   /api/demand-plans

GET    /api/replenishment/recommendations
POST   /api/replenishment/recalculate
PATCH  /api/replenishment/recommendations/{id}

GET    /api/purchase-requests
POST   /api/purchase-requests/{id}/confirm
```

Exact DTOs and contracts should be defined milestone by milestone.

---

## 11. Error handling

API errors should use a common structure, for example:

```json
{
  "timestamp": "2026-10-01T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid request",
  "path": "/api/products",
  "details": []
}
```

Important error cases:

- unknown shelf/device;
- invalid image;
- ML service unavailable;
- low recognition confidence;
- insufficient shelf capacity;
- product not found;
- unauthorized access;
- conflicting stock update.

---

## 12. Testing strategy

### Backend

- unit tests for business logic;
- controller/API tests;
- repository integration tests;
- integration tests with PostgreSQL/Testcontainers;
- security tests;
- tests for replenishment calculations.

### ML

- fixed validation image set;
- accuracy measurement for supported products;
- handling of empty shelf / multiple objects / poor image quality.

### End-to-end

At least these scenarios should be demonstrated:

1. Remove product -> image processed -> stock decreases.
2. Add product -> stock increases.
3. Search product -> shelf location returned.
4. Planned demand exceeds projected stock -> recommendation created.
5. Low-confidence recognition -> manual review instead of automatic stock mutation.

---

## 13. Definition of Done for the project

The diploma MVP is complete when:

- backend, frontend, database and ML service are integrated;
- at least one physical or simulated shelf can send images;
- supported products can be recognized with acceptable demo accuracy;
- inventory updates are recorded and visible in the UI;
- product location and placement recommendation work;
- planned demand and shelf life participate in replenishment logic;
- replenishment recommendations are generated;
- authorization is implemented;
- API is documented in Swagger/OpenAPI;
- project is deployed and can be demonstrated end-to-end;
- critical flows have automated tests;
- README contains setup and demo instructions.

---

## 14. Team responsibility

### Ivan Samal

Primary responsibility:

- Java/Spring backend;
- database design;
- inventory business logic;
- image-event processing integration;
- ML/CV integration;
- forecasting/replenishment logic.

### Uladzislau Mikhayevich

Primary responsibility:

- frontend web application;
- responsive UI;
- cloud/deployment work;
- API documentation and frontend integration.

Both developers participate in integration, testing and final demo preparation.

---

## 15. Main project risks

### Computer-vision accuracy

Risk: object quantity may be difficult to determine from a single camera angle.  
Mitigation: restrict MVP to several known products and controlled shelf conditions; preserve manual confirmation flow.

### Hardware integration

Risk: camera/controller work can consume too much time.  
Mitigation: define a simple HTTP image-upload protocol and support a simulator from week 2.

### Automatic purchasing

Risk: real suppliers may not expose an accessible API.  
Mitigation: implement internal purchase requests first; external supplier API is optional.

### Forecasting data shortage

Risk: no real historical warehouse dataset exists.  
Mitigation: support manually defined planned demand and generate synthetic historical data for forecasting demonstration.

### Scope growth

Risk: multi-warehouse, advanced AI and purchasing integrations may exceed 12 weeks.  
Mitigation: keep a strict MVP and treat integrations as extensions.
