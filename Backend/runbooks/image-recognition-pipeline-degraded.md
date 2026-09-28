# Runbook: Image recognition pipeline stuck or failing

## Symptom

`ImageEvent` rows accumulate in `RECEIVED`/`PROCESSING` status without reaching `PROCESSED`/`REVIEW`/`FAILED`, or a spike in `FAILED` events. Dashboard (Milestone 6, FR-17) shows a growing backlog of unprocessed events, or workers report stock not updating after removing/adding items.

## Impact

New stock changes from shelf cameras stop appearing (or are delayed). Manual inventory adjustments and reads still work — this does not corrupt existing stock data (by design, see [RFC-0001](../../Documentation/rfcs/0001-image-ingestion-and-recognition-pipeline.md) and NFR-02 in [PROJECT_DOCUMENTATION.md](../../Documentation/PROJECT_DOCUMENTATION.md)).

## Diagnosis

1. Check backend logs for the recognition worker: look for repeated errors calling the ML service (`log.error` in the recognition module — see [CODE_STYLE_BACKEND.md](../../Documentation/CODE_STYLE_BACKEND.md) for logging conventions).
2. Query stuck events:
   ```sql
   select id, shelf_id, device_id, processing_status, received_at
   from image_event
   where processing_status in ('RECEIVED', 'PROCESSING')
     and received_at < now() - interval '10 minutes'
   order by received_at;
   ```
3. Check whether the ML service is reachable and healthy (its own health endpoint / process status).
4. Check whether it's isolated to one shelf/device (device sending malformed images) vs. all shelves (ML service or backend worker itself is down).

## Mitigation

1. **ML service down/unreachable:** restart it; the backend's retry-with-backoff (see RFC-0001) will pick up `RECEIVED` events automatically once it's back — no manual replay needed unless events have exceeded the max retry count.
2. **Events exceeded max retries (status `FAILED`):** after fixing the root cause, requeue them:
   ```sql
   update image_event set processing_status = 'RECEIVED' where processing_status = 'FAILED' and id in (...);
   ```
3. **Single device sending bad data:** disable that device (`Device.status`) so it stops adding to the backlog while investigating, without affecting other shelves.
4. If stock accuracy is urgently needed for a demo before this is fixed: fall back to manual inventory adjustment (FR-08 / the manual correction endpoint) for the affected shelves.

## Escalation

Two-person team — escalate by pinging the other developer directly with: which shelves/devices are affected, the query results from step 2, and any error text from step 1.

## Related

- [RFC-0001: Image ingestion and recognition pipeline](../../Documentation/rfcs/0001-image-ingestion-and-recognition-pipeline.md)
- NFR-02 (Reliability) in [PROJECT_DOCUMENTATION.md](../../Documentation/PROJECT_DOCUMENTATION.md)
