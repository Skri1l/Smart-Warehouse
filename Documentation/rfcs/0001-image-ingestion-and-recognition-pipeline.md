# 0001. Image ingestion and recognition pipeline

Status: Draft
Author(s): Ivan Samal, Uladzislau Mikhayevich
Date: 2026-09-28
Related: Milestones 4-5, FR-05, FR-06, FR-07, FR-08, NFR-02

## Summary

Define the contract between shelf devices, the backend and the ML/CV service for turning a shelf image into a stock update, so both sides of the integration (device firmware/simulator, backend, ML service) can be built independently against an agreed interface before Milestone 4-5 implementation starts.

## Motivation

FR-05 through FR-08 require the backend to accept an image from a shelf device, hand it to an ML service, and turn the result into a stock change — without corrupting stock on a partial failure (NFR-02). This crosses three components owned differently (device/simulator, backend, ML service) and is exactly the kind of cross-boundary, hard-to-reverse contract [Documentation/rfcs/README.md](README.md) says should be designed before implementation, rather than discovered incrementally across two milestones.

## Detailed design

### 1. Device → Backend

`POST /api/v1/device-events/images` (device-authenticated, not user-authenticated — see [API_GUIDELINES.md](../API_GUIDELINES.md) for the auth model to extend).

Request: `multipart/form-data` with fields `shelfId`, `deviceId`, `capturedAt` (ISO-8601), and the image file.

Proposed additions from the device side ([ADR-0009](../adr/0009-device-transport-http-with-outbox.md), implemented in `Device/shelf_agent/transport/client.py`): an `X-Device-Key` header carrying `Device.deviceKey`; a `clientEventId` (UUID) field, unchanged across retries, for deduplication; `capturedAt` always in UTC with a `Z` suffix (parse as an instant, not `LocalDateTime`); image part named `image`. Devices retry `5xx`/`408`/`429`/network errors in capture order and do not retry other `4xx`.

On receipt, the backend:
1. Validates the device is registered and linked to the claimed shelf.
2. Persists the image and creates an `ImageEvent` row with `processingStatus = RECEIVED`.
3. Returns `202 Accepted` with the `ImageEvent` id immediately — recognition happens asynchronously (NFR-01 allows recognition to take longer than the ~2s budget for normal API calls).

### 2. Backend → ML service

Asynchronous worker picks up `RECEIVED` events and calls the ML service:

`POST {ml-service}/v1/recognize` with `{ "imageEventId": ..., "imageUrl": ... }`.

Response: `{ "detections": [{ "productSku": ..., "quantityDelta": ..., "confidence": 0.0-1.0 }], "modelVersion": ... }`.

### 3. Backend result handling

- `ImageEvent.processingStatus` transitions: `RECEIVED` → `PROCESSING` → `PROCESSED` | `FAILED` | `REVIEW`.
- If confidence ≥ configured threshold for every detection: create `RecognitionResult` (status `CONFIRMED`) and a `StockMovement` per product in one transaction; status → `PROCESSED`.
- If any detection is below threshold, or the ML service returns no detections: create `RecognitionResult` (status `PENDING_REVIEW`), do **not** touch `ShelfStock`; status → `REVIEW`. Satisfies FR-08.
- If the ML service call fails or times out: status → `FAILED`, retried with backoff up to N times before surfacing as a permanent failure (NFR-02 — failed events must be retryable and must not partially update stock).

### 4. Manual review

`PATCH /api/v1/recognition-results/{id}/confirm` lets a manager accept/correct/reject a `PENDING_REVIEW` result, which then applies (or discards) the stock movement the same way an automatic confirmation would.

## Alternatives considered

- **Synchronous recognition in the image-upload request** — rejected: recognition latency is unbounded (model inference, ML service load) and would block the device/simulator on every event; conflicts with NFR-01's async allowance.
- **ML service pushes results back to the backend (webhook) instead of the backend polling/worker pulling** — considered viable; deferred to the implementation ticket to decide based on whether the ML service is deployed as a long-running service or a batch job, since it doesn't change the backend's state machine above.

## Drawbacks

- Asynchronous processing means the frontend must poll or be notified when an `ImageEvent` finishes processing, rather than getting the result in the upload response — adds a bit of frontend complexity (Milestone 6 dashboard needs a "processing" state in the UI).
- Retry-with-backoff requires a scheduled job/queue in the backend, which is new infrastructure not present in the Milestone 1-3 skeleton.

## Unresolved questions

- Exact confidence threshold value(s) — likely needs tuning once the demo product dataset (Milestone 5) exists; store as configuration, not a constant.
- Whether image storage is local disk, object storage, or DB blob — a capacity/deployment decision for the Milestone 8 cloud deployment, doesn't block this RFC.

## Outcome

_(to be filled in once Milestone 4-5 implementation lands)_
