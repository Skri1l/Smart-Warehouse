# Smart Warehouse — 12-week Milestone Plan

The project is divided into **8 milestones**. This gives enough checkpoints without turning every week into an artificial milestone. Each milestone has a concrete deliverable that can later be expanded into a separate technical specification.

---

## Milestone 1 — Project foundation and architecture
**Weeks:** 1

### Goal
Create the technical foundation and freeze the MVP scope.

### Deliverables
- monorepo structure: `Backend`, `Frontend`, `Documentation`;
- architecture diagram;
- agreed technology stack;
- initial domain model;
- PostgreSQL connection;
- Spring Boot backend skeleton;
- frontend application skeleton;
- Docker Compose for required local services;
- base README;
- coding/branching conventions.

### Acceptance criteria
- backend and frontend start locally;
- backend connects to PostgreSQL;
- repository can be cloned and started by both developers;
- core entities and MVP boundaries are documented.

---

## Milestone 2 — Core warehouse and inventory backend
**Weeks:** 2–3

### Goal
Implement the base business domain without computer vision.

### Deliverables
- database migrations;
- entities and repositories for warehouses, racks, shelves, products and stock;
- CRUD API for products and warehouse structure;
- inventory balance model;
- stock movement history;
- manual inventory adjustment endpoint;
- validation and unified error handling;
- backend unit/integration tests;
- first Swagger/OpenAPI specification.

### Acceptance criteria
- products and shelves can be created;
- stock can be manually added/removed;
- every change creates history;
- current quantity can be queried via API;
- core API tests pass.

---

## Milestone 3 — Authentication, roles and basic frontend
**Weeks:** 4

### Goal
Make the system usable by authenticated warehouse users.

### Deliverables
- authentication;
- roles: worker / manager-admin;
- secured backend endpoints;
- frontend login;
- main layout/navigation;
- product list;
- shelf/stock view;
- responsive base UI.

### Acceptance criteria
- unauthorized users cannot access protected resources;
- role restrictions work;
- user can log in and see current stock in the frontend.

---

## Milestone 4 — Shelf device and image-event pipeline
**Weeks:** 5

### Goal
Connect shelf cameras/controllers to the backend independently from the final ML model.

### Deliverables
- device registration/authentication;
- endpoint for image upload;
- shelf/device metadata validation;
- image-event persistence;
- image storage strategy;
- event statuses: received / processing / processed / failed / review;
- simple device simulator;
- optional physical camera/controller prototype.

### Acceptance criteria
- a simulated or physical shelf sends an image;
- backend knows which shelf produced it;
- image/event can be traced in the database;
- failed uploads do not modify inventory.

---

## Milestone 5 — Computer vision and automatic stock updates
**Weeks:** 6–7

### Goal
Recognize supported products from shelf images and update inventory automatically.

### Deliverables
- ML/CV service;
- REST contract backend <-> ML service;
- selected demo product dataset;
- product detection/counting logic;
- confidence score;
- backend recognition-result entity;
- automatic creation of stock movement;
- manual review flow for low-confidence results;
- frontend page for recognition events/errors.

### Acceptance criteria
- supported demo images are processed automatically;
- system identifies/counts configured products with agreed demo accuracy;
- valid result updates stock;
- low-confidence result does not silently change stock;
- each automatic update is traceable to its image.

---

## Milestone 6 — Product location and placement recommendations
**Weeks:** 8

### Goal
Implement warehouse navigation features for workers.

### Deliverables
- product-location API;
- visual shelf/rack map or structured shelf view;
- product search;
- “where is this product?” flow;
- shelf-capacity rules;
- “where should I place incoming goods?” recommendation;
- frontend worker flow.

### Acceptance criteria
- worker can search for a product and see its shelf;
- system recommends a valid shelf for incoming goods;
- recommendation respects configured capacity/basic placement rules.

---

## Milestone 7 — Demand planning, shelf life and replenishment
**Weeks:** 9–10

### Goal
Implement the intelligent replenishment part of the diploma.

### Deliverables
- product shelf-life configuration;
- planned-demand entity/API/UI;
- consumption-history aggregation;
- first forecasting strategy;
- scheduler;
- projected-stock calculation;
- shelf-life-aware order timing;
- replenishment recommendation entity;
- recommendation UI;
- confirmation/rejection flow;
- optional internal purchase request generation.

### Acceptance criteria
- manager can define required quantity/date;
- scheduler evaluates future demand;
- insufficient projected stock creates a recommendation;
- recommendation contains quantity and required date;
- shelf life affects when the system recommends ordering;
- recommendation can be accepted/rejected.

---

## Milestone 8 — Integration, cloud, testing and diploma demo
**Weeks:** 11–12

### Goal
Turn the individual modules into a stable demonstrable system.

### Deliverables
- cloud deployment;
- production-like database;
- integrated frontend/backend/ML;
- final OpenAPI documentation;
- end-to-end tests;
- bug fixing;
- security review;
- logging and basic monitoring;
- demo dataset;
- installation/deployment documentation;
- final README;
- demo scenario and presentation preparation.

### Acceptance criteria
The following end-to-end demo works:

1. shelf/device sends image;
2. product is recognized;
3. stock changes in database;
4. frontend shows updated stock and history;
5. user can locate the product;
6. future demand is configured;
7. scheduler detects shortage;
8. replenishment recommendation is created;
9. manager reviews it.

The system is deployed and accessible for diploma demonstration.

---

# Timeline summary

| Week | Main focus |
|---|---|
| 1 | Foundation and architecture |
| 2–3 | Warehouse + inventory backend |
| 4 | Security + basic frontend |
| 5 | Camera/device/image pipeline |
| 6–7 | Computer vision + automatic inventory |
| 8 | Product location + placement |
| 9–10 | Forecasting + replenishment |
| 11–12 | Integration + cloud + testing + demo |

---

# Recommended order for future milestone specifications

For every milestone, create a separate technical specification containing:

1. business goal;
2. user stories;
3. exact backend tasks;
4. exact frontend tasks;
5. database changes;
6. API endpoints and DTOs;
7. validation and error cases;
8. security rules;
9. tests;
10. Definition of Done;
11. responsibilities of each developer.
