# Price Service with gRPC Integration - Implementation Tracker

## Progress

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | DONE | Shared gRPC Proto Module (`grpc-proto/`) |
| Phase 2 | DONE | Price Service (`price-service/`) |
| Phase 3 | DONE | Property Service Changes (gRPC client) |
| Phase 4 | DONE | Analytics Service Changes (Kafka consumer) |
| Phase 5 | DONE | Kong Gateway Changes |
| Phase 6 | DONE | Frontend Changes |
| Phase 7 | DONE | Scripts Updates |

---

## Phase 3: Property Service Changes (DONE)

Add gRPC client to `property-service` to call `price-service`. Fallback to cached price if unavailable.

- Add deps to `property-service/pom.xml`: `grpc-proto`, `grpc-client-spring-boot-starter`
- New file: `grpc/PriceGrpcClient.java` — uses `@GrpcClient("price-service")` with `PriceServiceBlockingStub`
  - Methods: `getCurrentPrice(Long)`, `batchGetPrices(List<Long>)`, `updatePrice(...)`
  - `@CircuitBreaker(name = "priceService")` on all methods
  - Fallbacks: return `Optional.empty()` / empty list on reads, throw on writes
- Modify `PropertyService.java`:
  - `createProperty()`: call `priceGrpcClient.updatePrice()` after save
  - `updateProperty()`: call `priceGrpcClient.updatePrice()` if price changed
  - `getPropertyById()`: attempt gRPC fresh price, fallback to cached `property.getPrice()`
  - `getAllProperties()` / `searchProperties()`: batch gRPC call, fallback to cached
- Update `application.yml`: add `grpc.client.price-service` config + resilience4j circuit breaker
- Update `docker-compose.yml`: add `PRICE_SERVICE_GRPC_HOST/PORT` env vars

## Phase 4: Analytics Service Changes (DONE)

Consume `price-change-events` from Kafka, persist to MongoDB.

- New: `dto/PriceChangeEvent.java` — Kafka event DTO
- New: `entity/PriceChangeRecord.java` — MongoDB document (`@Document("price_changes")`)
- New: `repository/PriceChangeRepository.java` — `MongoRepository<PriceChangeRecord, String>`
- New: `consumer/PriceChangeEventConsumer.java` — `@KafkaListener(topics = "price-change-events")`
- No config changes needed (existing `trusted.packages: "*"` handles it)

## Phase 5: Kong Gateway Changes (DONE)

Update `kong/config/kong-local.yaml`:

- Add service: `price-service` -> `http://host.docker.internal:8084`
- Add routes:
  - `prices-public-get`: GET `/api/prices` (public)
  - `prices-protected`: PUT `/api/prices` (authenticated)
  - `health-price-service`: GET `/api/prices/actuator/health`

## Phase 6: Frontend Changes (DONE)

- Add types to `frontend/types/api.ts`: `PriceResponse`, `PricePointResponse`, `PriceHistoryResponse`, `UpdatePriceRequest`
- Create `frontend/lib/api/priceApi.ts`: singleton API class (follows `propertyApi.ts` pattern)
- Create `frontend/components/PriceHistory.tsx`: client component showing price change timeline
- Update `frontend/app/properties/[id]/page.tsx`: add `<PriceHistory>` component

## Phase 7: Scripts Updates (DONE)

- `scripts/start-all-services.sh`: add Step 6 for price-service + health check
- `scripts/stop-all-services.sh`: add price-service stop/cleanup
- `scripts/status-all-services.sh`: add price-db (5435) and price-service (8084) checks
