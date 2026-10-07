# 0009. Devices push images over HTTP with an ordered on-device outbox

Status: Proposed
Date: 2026-10-07

## Context

[RFC-0001](../rfcs/0001-image-ingestion-and-recognition-pipeline.md) defines the upload as `POST /api/v1/device-events/images` (multipart: `shelfId`, `deviceId`, `capturedAt`, image), answered with `202 Accepted`. It leaves open how a device behaves when the backend is unreachable, and how it authenticates.

Relevant facts:

- NFR-02: a failure must not corrupt stock, and failed events must be retryable.
- The backend derives a stock **delta** from consecutive images of the same shelf. If frames of one shelf arrive out of order, the deltas are wrong, and the error is silent.
- Shelf Wi-Fi in a warehouse is unreliable, and a Pi can lose power.
- A retried upload may already have been stored by the backend: the response was lost, not the request.
- The `Device` entity already has a unique `deviceKey`, and `DeviceController` already exposes `PATCH /devices/{id}/heartbeat`.

## Decision

We will use plain HTTP push from device to backend, as in RFC-0001, with these device-side rules:

1. **Persistent outbox, strict order.** A frame that cannot be delivered (no connection, timeout, `5xx`, `408`, `429`) is written to an on-disk FIFO. While the outbox is non-empty, new frames of that shelf are queued behind it, never sent ahead. The outbox is flushed before each new capture and after every successful heartbeat.
2. **Final errors are kept, not retried.** Any other `4xx` means the request itself is wrong (unknown device, bad key). The frame moves to `outbox/rejected/` and is logged as an error.
3. **At-least-once delivery with a client event id.** Each frame carries a `clientEventId` (UUID) field, unchanged across retries, so the backend can deduplicate.
4. **Device authentication by header.** Every request carries `X-Device-Key: <Device.deviceKey>`. The backend checks that the key belongs to the claimed `deviceId` and that it is linked to `shelfId`.
5. **`capturedAt` is UTC ISO-8601 with a `Z` suffix** and millisecond precision, set at capture time and preserved through retries.
6. **Liveness via the existing heartbeat endpoint**, every 30 s by default.

Items 3–5 extend RFC-0001's contract. The backend endpoint should accept `clientEventId`, read `X-Device-Key` and parse `capturedAt` as an instant (`OffsetDateTime`/`Instant`, not `LocalDateTime`).

## Alternatives considered

- **MQTT broker (Mosquitto) between devices and backend**: rejected for the MVP. It gives buffering and QoS, but adds a broker to deploy and secure (Milestone 8), a second protocol for the backend, and 0.5–5 MB images are an awkward fit for MQTT payloads. The HTTP contract is already agreed in RFC-0001.
- **Fire-and-forget HTTP (no outbox)**: rejected. Any Wi-Fi drop loses a shelf event and breaks NFR-02.
- **In-memory retry queue only**: rejected. A power cycle, common on a shelf controller, loses everything queued.
- **Send the newest frame first, backfill later**: rejected. It corrupts stock deltas, which depend on frame order.
- **Device auth with a user JWT or mTLS**: rejected for now. A user token doesn't model a device, and mTLS needs certificate provisioning per Pi. A per-device key over HTTPS is enough for the diploma and can be upgraded later behind the same header check.

## Consequences

- No image is lost to a network or backend outage. Each shelf's frames reach the backend in capture order, possibly late. The backend must order by `capturedAt`, not by arrival time.
- Delivery is at-least-once. The backend needs a unique constraint or lookup on `clientEventId` to stay idempotent. Until it has one, a retry after a lost response can create a duplicate `ImageEvent`.
- One shelf whose backend calls keep failing with retryable errors stalls only its own queue. Other shelves are independent.
- The outbox needs disk space on the Pi. Images are small compared with an SD card, but there is no size cap yet. Add one if long outages become realistic.
- The backend has to implement `X-Device-Key` validation. Until then, the header is sent and ignored.
- All of this lives in `Device/shelf_agent/transport/` and `agent.py`, shared by the device and the emulator ([ADR-0008](0008-shelf-agent-shared-core.md)). The emulator demonstrates the same retry behaviour: stop the backend, keep sending, start it again.
