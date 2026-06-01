# Product Requirements Proposal (PRP)
# Property Map Feature — Embedded Location Map on Property Detail Page

**Version**: 1.0
**Date**: 2026-06-01
**Author**: Engineering Team
**Status**: Draft — Pending Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Objectives](#3-goals-and-objectives)
4. [User Stories](#4-user-stories)
5. [Technical Architecture](#5-technical-architecture)
6. [Provider Abstraction (Free → Paid Switch)](#6-provider-abstraction-free--paid-switch)
7. [Database Schema & Migration](#7-database-schema--migration)
8. [Backend: Property Service Changes](#8-backend-property-service-changes)
9. [Backend: Geocoding Service](#9-backend-geocoding-service)
10. [Frontend: Map Provider Interface](#10-frontend-map-provider-interface)
11. [Frontend: PropertyMap Component](#11-frontend-propertymap-component)
12. [Frontend: Property Page Integration](#12-frontend-property-page-integration)
13. [Internationalization (i18n)](#13-internationalization-i18n)
14. [Backfill Script for Existing Properties](#14-backfill-script-for-existing-properties)
15. [Configuration](#15-configuration)
16. [Privacy, Attribution & Rate Limits](#16-privacy-attribution--rate-limits)
17. [Implementation Plan](#17-implementation-plan)
18. [Switching to Google Maps Later — Checklist](#18-switching-to-google-maps-later--checklist)
19. [Out of Scope](#19-out-of-scope)
20. [Success Metrics](#20-success-metrics)

---

## 1. Executive Summary

### 1.1 Overview

This PRP adds an **embedded location map** to the property detail page. Each property will store geographic coordinates (latitude, longitude) derived automatically from its address. The detail page will render a Leaflet-based map (using free OpenStreetMap tiles) immediately above the comment section, showing a single marker at the property's location.

The system is built behind a **provider abstraction layer** on both backend (geocoding) and frontend (map rendering) so the team can swap from the free OpenStreetMap/Nominatim stack to a paid provider (Google Maps, Mapbox, MapTiler) by changing a single environment variable — no business-logic changes required.

### 1.2 Current State

```
Property Detail Page (frontend/app/properties/[id]/page.tsx)
├── Image gallery
├── Title / address / price
├── Stats (bedrooms, bathrooms, area, type)
├── Description
├── Features
├── Price history
└── Comments
        ▲
        └── (No map. Address is shown as a plain text string only.)

Property DB schema
└── address (VARCHAR), city (VARCHAR) — no lat/lng
```

### 1.3 Target State

```
┌──────────────────────────────────────────────────────────────────┐
│                  Property Detail Page                             │
│  ...existing sections...                                          │
│  ├── Price history                                                │
│  ├── 🆕 Location map (PropertyMap component)  ◄── new            │
│  └── Comments                                                     │
└──────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌────────────────┐   ┌──────────────┐   ┌──────────────────────┐
│ MapProvider    │──▶│  Leaflet     │   │  GoogleMapsProvider  │
│ (interface)    │   │  Provider    │   │  (later, swappable)  │
└────────────────┘   │  (default)   │   └──────────────────────┘
                     └──────────────┘
                          │
                          ▼
                  OpenStreetMap tiles
                  (free, no API key)


┌────────────────┐   ┌──────────────┐   ┌──────────────────────┐
│ Property       │──▶│ Geocoder     │──▶│ NominatimGeocoder    │
│ Service        │   │ (interface)  │   │ (default, free)      │
│ on create/edit │   └──────────────┘   ├──────────────────────┤
└────────────────┘                       │ GoogleGeocoder       │
                                         │ (later, swappable)   │
                                         └──────────────────────┘
```

### 1.4 Key Benefits

| Benefit | Description |
|---------|-------------|
| **Zero ongoing cost (initially)** | Leaflet + OSM tiles + Nominatim geocoding are all free, no API key required |
| **Provider portability** | Single env var (`MAP_PROVIDER`) swaps the entire stack to Google Maps or Mapbox |
| **Better property discovery** | Users can visually verify location before contacting the seller |
| **Vendor-neutral data model** | Stored coords (lat/lng + WGS84) are portable across providers |

---

## 2. Problem Statement

### 2.1 Current Limitations

1. **No visual location context.** The address is shown only as text. Users cannot tell whether a property is in a desirable neighborhood, near transit, or close to schools without copying the address into a separate map app.
2. **Trust gap.** A real address with no map raises the risk that listings are vague or wrong — competitors all show maps.
3. **No persisted geodata.** Even if we wanted to add a map today, the database has no coordinates, so we'd have to geocode on every page load.
4. **Vendor lock-in risk.** If we naively call Google Maps APIs from page code, we cannot switch providers without rewriting components.

### 2.2 Why Now

- We have ~1,000s of properties with addresses but no map UX.
- Competitor parity: nearly all real estate listing sites embed maps.
- Recent comment-service work has already established the "service + API client + i18n" pattern we can follow.

---

## 3. Goals and Objectives

### 3.1 Goals

| ID | Goal | Measure |
|----|------|---------|
| G-1 | Display an embedded map on every property detail page where coordinates exist | 100% of properties with `latitude IS NOT NULL` render a map |
| G-2 | Populate coordinates automatically from address | ≥ 90% of new properties have coords within 60s of creation |
| G-3 | Backfill all existing properties | ≥ 90% success rate from one-time backfill script |
| G-4 | Make provider swap a config change, not a code change | Switching to Google Maps takes ≤ 1 day of work, no React component edits |

### 3.2 Non-Goals

- Map-based property search / "draw a polygon" filtering (separate future PRP).
- Nearby-amenities overlays (schools, transit) — out of scope for v1.
- Directions / route planning — we'll provide a deep link to native maps apps instead.
- Manual lat/lng entry by users in v1 (auto-geocode only; manual override is a future enhancement).

---

## 4. User Stories

| ID | Story | Priority |
|----|-------|----------|
| US-1 | As a buyer, I want to see the property's location on a map so I can judge the neighborhood | High |
| US-2 | As a buyer, I want to click the marker and see the property's address in a popup | High |
| US-3 | As a buyer, I want to open the location in my native maps app for directions | Medium |
| US-4 | As a seller, when I create or edit a property, the system should pick up the coordinates from my address automatically | High |
| US-5 | As an operator, I want a backfill job that geocodes all existing properties without manual intervention | High |
| US-6 | As a tech lead, I want to switch from OpenStreetMap to Google Maps by changing one env var | High |

---

## 5. Technical Architecture

### 5.1 High-Level Flow

**On property create/update:**
```
POST /api/properties  ──▶  PropertyService.save()
                                │
                                ├── persist Property (status=PENDING_GEOCODE)
                                └── async ──▶ GeocodingService.geocode(address, city)
                                                    │
                                                    ▼
                                            NominatimGeocoder
                                                    │
                                                    ▼
                                           {lat, lng, confidence}
                                                    │
                                                    ▼
                                  Property.{latitude, longitude, geocodingStatus=SUCCESS, geocodedAt=now}
```

**On property detail page view:**
```
Frontend  ──▶  GET /api/properties/{id}
                       │
                       ▼ (response now includes latitude, longitude, geocodingStatus)
              <PropertyMap lat lng address /> (client component, dynamic import)
                       │
                       ▼
              MapProvider (chosen by NEXT_PUBLIC_MAP_PROVIDER env var)
                       │
                       ▼
              LeafletMapProvider — renders OSM tiles
```

### 5.2 Components Touched

| Layer | Component | Change Type |
|-------|-----------|-------------|
| DB | `properties` table | Add 4 columns + migration |
| Backend | `Property` entity (`property-service`) | Add fields |
| Backend | `PropertyDTO` | Add fields |
| Backend | `PropertyService` | Trigger geocoding async on create/update |
| Backend | `GeocodingService` (new interface + Nominatim impl) | New |
| Backend | `BackfillCommand` (new CLI runner) | New |
| Frontend | `types/api.ts` | Add `latitude`, `longitude` to `Property` |
| Frontend | `lib/maps/` directory | New — provider interface + Leaflet impl |
| Frontend | `components/PropertyMap.tsx` | New |
| Frontend | `app/properties/[id]/page.tsx` | Insert map between `<PriceHistory>` and `<PropertyComments>` |
| Frontend | `messages/{en,vi}.json` | Add `maps.*` strings |
| Frontend | `package.json` | Add `leaflet`, `react-leaflet`, types |

---

## 6. Provider Abstraction (Free → Paid Switch)

This is the core architectural decision. Two abstractions are needed:

### 6.1 Backend Geocoding Provider

```java
public interface GeocodingProvider {
    GeocodeResult geocode(String address, String city);
}

public record GeocodeResult(
    double latitude,
    double longitude,
    double confidence,  // 0.0–1.0
    String provider     // "nominatim", "google", "mapbox" — for audit
) {}
```

Implementations:
- `NominatimGeocodingProvider` (v1, default) — calls `https://nominatim.openstreetmap.org/search`
- `GoogleGeocodingProvider` (future) — calls Google Geocoding API
- `MapboxGeocodingProvider` (future) — calls Mapbox Geocoding API

Selection via `application.yml`:

```yaml
app:
  maps:
    geocoding-provider: ${MAP_GEOCODING_PROVIDER:nominatim}
    nominatim:
      base-url: https://nominatim.openstreetmap.org
      user-agent: "RealEstateApp/1.0 (contact@example.com)"
      rate-limit-per-second: 1
    google:
      api-key: ${GOOGLE_MAPS_API_KEY:}
```

Spring `@ConditionalOnProperty` wires the right bean.

### 6.2 Frontend Map Provider

```ts
// frontend/lib/maps/types.ts
export interface MapProviderProps {
  latitude: number;
  longitude: number;
  zoom?: number;
  marker?: { title: string; description?: string };
  className?: string;
}

export interface MapProvider {
  /** React component that renders the actual map */
  MapView: React.ComponentType<MapProviderProps>;
  /** Display name for attribution UI */
  name: string;
  /** Build a deep link for "open in external app" */
  externalLink: (lat: number, lng: number, label?: string) => string;
}
```

Implementations:
- `LeafletProvider` (v1, default) — uses `react-leaflet` + OSM tiles
- `GoogleMapsProvider` (future) — uses `@react-google-maps/api`
- `MapboxProvider` (future) — uses `react-map-gl`

Selection at module-load time:

```ts
// frontend/lib/maps/index.ts
import { LeafletProvider } from "./providers/leaflet";

const PROVIDERS = {
  leaflet: () => import("./providers/leaflet").then(m => m.LeafletProvider),
  google:  () => import("./providers/google").then(m => m.GoogleMapsProvider),
  mapbox:  () => import("./providers/mapbox").then(m => m.MapboxProvider),
} as const;

const provider = process.env.NEXT_PUBLIC_MAP_PROVIDER ?? "leaflet";
export const mapProvider = await PROVIDERS[provider]();
```

Each provider lives in its own file so unused providers are tree-shaken out.

### 6.3 What the consumer code sees

`PropertyMap.tsx` only imports `mapProvider` and renders `<mapProvider.MapView ... />`. It does not import Leaflet, Google, or Mapbox directly. **This is the contract that lets us swap providers without touching feature code.**

---

## 7. Database Schema & Migration

### 7.1 New columns on `properties`

```sql
ALTER TABLE properties
    ADD COLUMN latitude         DOUBLE PRECISION,
    ADD COLUMN longitude        DOUBLE PRECISION,
    ADD COLUMN geocoded_at      TIMESTAMP,
    ADD COLUMN geocoding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

CREATE INDEX idx_properties_geocoding_status ON properties (geocoding_status);

-- Optional but recommended for future "properties in bounding box" queries
CREATE INDEX idx_properties_lat_lng ON properties (latitude, longitude)
    WHERE latitude IS NOT NULL;
```

Columns:

| Column | Type | Notes |
|--------|------|-------|
| `latitude` | `DOUBLE PRECISION` | Nullable — null until geocoded |
| `longitude` | `DOUBLE PRECISION` | Nullable |
| `geocoded_at` | `TIMESTAMP` | When the coords were last computed |
| `geocoding_status` | `VARCHAR(20)` | `PENDING` / `SUCCESS` / `FAILED` / `SKIPPED` |

### 7.2 Migration

If `property-service` uses Flyway: create `V{n}__add_property_coordinates.sql` under `property-service/src/main/resources/db/migration/`. If it relies on `hibernate.ddl-auto=update`, the columns will be added at boot, but a versioned migration is still recommended for production.

---

## 8. Backend: Property Service Changes

### 8.1 Entity update

File: `property-service/src/main/java/com/realestate/backend/entity/Property.java`

Add:

```java
@Column
private Double latitude;

@Column
private Double longitude;

@Column(name = "geocoded_at")
private LocalDateTime geocodedAt;

@Enumerated(EnumType.STRING)
@Column(name = "geocoding_status", nullable = false)
private GeocodingStatus geocodingStatus = GeocodingStatus.PENDING;
```

Add `GeocodingStatus` enum: `PENDING`, `SUCCESS`, `FAILED`, `SKIPPED`.

### 8.2 DTO update

Add `latitude`, `longitude`, `geocodingStatus` to `PropertyDTO` (response) so the frontend can read them. Do **not** accept lat/lng on the request DTO in v1 — coords are server-derived.

### 8.3 Service hook

In `PropertyService.create()` and `PropertyService.update()` (only if address or city changed):

```java
@Transactional
public Property create(CreatePropertyRequest req, String userId) {
    Property p = ...persist...;
    geocodingTaskQueue.enqueue(p.getId(), p.getAddress(), p.getCity());
    return p;
}
```

The geocoding call is **asynchronous** (via `@Async` or a Kafka producer) so a slow Nominatim response never blocks property creation. The producer publishes to a topic like `property.geocode.requested` and a consumer in the same service handles it. (Kafka is already in the stack — see `kafka/` directory.)

### 8.4 Endpoint additions (optional)

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/properties/{id}/geocode` | Admin-only: re-trigger geocoding for one property |

---

## 9. Backend: Geocoding Service

### 9.1 Nominatim client

File: `property-service/src/main/java/com/realestate/backend/maps/NominatimGeocodingProvider.java`

Uses Spring's `RestClient` (or `WebClient`). Endpoint:

```
GET https://nominatim.openstreetmap.org/search
    ?q=<address>,<city>
    &format=json
    &limit=1
    &addressdetails=0
```

Required headers (per Nominatim policy):
- `User-Agent: RealEstateApp/1.0 (contact@example.com)`
- `Accept-Language: vi,en`

### 9.2 Rate limiting

Nominatim's free public endpoint allows **≤ 1 request/second**. Use a token bucket (Resilience4j RateLimiter, or a simple `Semaphore` + scheduled refill). Sleep when budget exhausted.

For higher throughput (>1 req/sec) options later:
- Self-host Nominatim (Docker) → unlimited
- MapTiler / LocationIQ — free tiers ~5k req/day

### 9.3 Failure handling

| Outcome | DB state |
|---------|----------|
| HTTP 200 + result | `status=SUCCESS`, lat/lng populated |
| HTTP 200 + empty result | `status=FAILED`, lat/lng null |
| HTTP error / timeout | `status=PENDING` (retry up to 3 times via Kafka redrive) → after 3 fails, `status=FAILED` |
| Address is whitespace-only | `status=SKIPPED` |

Log all failures with the address so a human can investigate addresses that consistently fail.

---

## 10. Frontend: Map Provider Interface

### 10.1 Directory layout

```
frontend/lib/maps/
├── index.ts                    # Picks provider based on env var
├── types.ts                    # MapProvider, MapProviderProps interfaces
└── providers/
    ├── leaflet.tsx             # v1 default
    ├── google.tsx              # stub (throws "not yet implemented")
    └── mapbox.tsx              # stub
```

### 10.2 Leaflet provider

`leaflet.tsx`:

```tsx
"use client";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import type { MapProvider, MapProviderProps } from "../types";

function LeafletMapView({ latitude, longitude, zoom = 15, marker, className }: MapProviderProps) {
  return (
    <MapContainer
      center={[latitude, longitude]}
      zoom={zoom}
      className={className ?? "h-96 w-full rounded-lg"}
      scrollWheelZoom={false}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Marker position={[latitude, longitude]}>
        {marker && (
          <Popup>
            <strong>{marker.title}</strong>
            {marker.description && <p>{marker.description}</p>}
          </Popup>
        )}
      </Marker>
    </MapContainer>
  );
}

export const LeafletProvider: MapProvider = {
  MapView: LeafletMapView,
  name: "OpenStreetMap",
  externalLink: (lat, lng, label) =>
    `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`,
};
```

### 10.3 Dependencies

```bash
npm install leaflet react-leaflet
npm install -D @types/leaflet
```

The OSM tile server URL is in **one place** (this file). If we later switch to MapTiler / Stadia for higher quotas without changing providers, we change only the `url` string.

---

## 11. Frontend: PropertyMap Component

File: `frontend/components/PropertyMap.tsx`

```tsx
"use client";
import dynamic from "next/dynamic";
import { useTranslations } from "next-intl";
import { mapProvider } from "@/lib/maps";

const MapView = dynamic(
  () => Promise.resolve(mapProvider.MapView),
  { ssr: false, loading: () => <MapSkeleton /> },
);

interface Props {
  latitude: number | null;
  longitude: number | null;
  address: string;
  geocodingStatus: "PENDING" | "SUCCESS" | "FAILED" | "SKIPPED";
}

export function PropertyMap({ latitude, longitude, address, geocodingStatus }: Props) {
  const t = useTranslations("maps");

  if (latitude == null || longitude == null) {
    return (
      <section className="my-6">
        <h2 className="text-xl font-semibold mb-3">{t("heading")}</h2>
        <p className="text-gray-500 text-sm">
          {geocodingStatus === "PENDING" ? t("pending") : t("unavailable")}
        </p>
      </section>
    );
  }

  return (
    <section className="my-6">
      <h2 className="text-xl font-semibold mb-3">{t("heading")}</h2>
      <MapView
        latitude={latitude}
        longitude={longitude}
        marker={{ title: address }}
        className="h-96 w-full rounded-lg border"
      />
      <div className="mt-2 flex items-center justify-between text-sm text-gray-600">
        <span>{t("attribution", { provider: mapProvider.name })}</span>
        <a
          href={mapProvider.externalLink(latitude, longitude, address)}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-600 hover:underline"
        >
          {t("openExternal")}
        </a>
      </div>
    </section>
  );
}
```

**Why `dynamic` with `ssr: false`:** Leaflet touches `window` on import; rendering it on the server crashes Next.js. The dynamic import + skeleton fallback is the standard Next.js pattern.

---

## 12. Frontend: Property Page Integration

File: `frontend/app/properties/[id]/page.tsx`

Between the existing `<PriceHistory>` (line 247) and `<PropertyComments>` (line 293), insert:

```tsx
<PropertyMap
  latitude={property.latitude}
  longitude={property.longitude}
  address={`${property.address}, ${property.city}`}
  geocodingStatus={property.geocodingStatus}
/>
```

Layout: keep it inside the main grid's left column (`lg:col-span-2`) so it lines up with the description / features / comments sections.

Also update `frontend/types/api.ts`:

```ts
export interface Property {
  // ...existing fields...
  latitude: number | null;
  longitude: number | null;
  geocodingStatus: "PENDING" | "SUCCESS" | "FAILED" | "SKIPPED";
}
```

---

## 13. Internationalization (i18n)

Add to `frontend/messages/en.json`:

```json
"maps": {
  "heading": "Location",
  "pending": "Map will appear shortly — we're looking up this address.",
  "unavailable": "Map unavailable for this property.",
  "attribution": "Map data © {provider}",
  "openExternal": "Open in maps"
}
```

Add to `frontend/messages/vi.json`:

```json
"maps": {
  "heading": "Vị trí",
  "pending": "Bản đồ sẽ xuất hiện trong giây lát — chúng tôi đang tra cứu địa chỉ này.",
  "unavailable": "Bản đồ không khả dụng cho bất động sản này.",
  "attribution": "Dữ liệu bản đồ © {provider}",
  "openExternal": "Mở trong ứng dụng bản đồ"
}
```

---

## 14. Backfill Script for Existing Properties

A Spring `CommandLineRunner` (or a separate `@SpringBootApplication` class) that:

1. Loads all properties where `geocoding_status = 'PENDING'`.
2. For each, calls `GeocodingProvider.geocode(address, city)`.
3. Updates the row.
4. Sleeps to respect the 1 req/sec Nominatim limit (~17 minutes per 1,000 properties).
5. Logs success/failure counts.

Invocation:

```bash
# Production
java -jar property-service.jar --spring.profiles.active=backfill

# Local dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=backfill
```

Make it **idempotent** — running it twice should only retry the FAILED + PENDING rows, not re-geocode SUCCESS rows.

---

## 15. Configuration

### 15.1 Backend env vars

| Variable | Default | Purpose |
|----------|---------|---------|
| `MAP_GEOCODING_PROVIDER` | `nominatim` | Which `GeocodingProvider` bean to wire |
| `NOMINATIM_BASE_URL` | `https://nominatim.openstreetmap.org` | Override for self-hosted |
| `NOMINATIM_USER_AGENT` | `RealEstateApp/1.0 (...)` | Required by Nominatim policy |
| `GOOGLE_MAPS_API_KEY` | (empty) | Future: enables `google` provider |

### 15.2 Frontend env vars

| Variable | Default | Purpose |
|----------|---------|---------|
| `NEXT_PUBLIC_MAP_PROVIDER` | `leaflet` | Which `MapProvider` to load |
| `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` | (empty) | Future: needed when provider=`google` |

---

## 16. Privacy, Attribution & Rate Limits

### 16.1 Attribution (required for OSM)

- Leaflet `TileLayer` already renders the `© OpenStreetMap contributors` overlay — do not hide it.
- The `attribution` i18n string also mentions the provider visibly under the map.

### 16.2 Nominatim usage policy

We must comply with [Nominatim's usage policy](https://operations.osmfoundation.org/policies/nominatim/):
- Identify the app via `User-Agent` header (real contact email).
- Maximum 1 request/second.
- Cache results (we persist coords in our DB — already compliant).
- No bulk geocoding without prior approval — our backfill is below this threshold (~thousands of addresses).

If we anticipate sustained heavy use, self-host Nominatim (single Docker container) or move to a paid geocoder.

### 16.3 Privacy — exact vs. approximate coordinates

For v1 we show the exact geocoded point. A future option (out of scope) is to fuzz coordinates by ~50–100 m for properties marked "private location" — useful when sellers don't want the precise pin shown.

### 16.4 Tile server quota

OSM's public tile server has a soft cap ("not heavy commercial use"). Once we exceed ~10k tile loads/day in production, swap the tile URL in `LeafletProvider` to a free-tier provider (MapTiler 100k/month, or Stadia). This is **one string change**.

---

## 17. Implementation Plan

### Phase 1 — Backend foundation (2 days)

1. Add columns + JPA fields + `GeocodingStatus` enum + Flyway migration.
2. Update `PropertyDTO` to expose lat/lng/status.
3. Create `GeocodingProvider` interface + `NominatimGeocodingProvider` impl with rate limiting.
4. Wire async hook on `PropertyService.create()` and `update()`.
5. Unit tests for the geocoder (mocked HTTP) and PropertyService geocoding trigger.

### Phase 2 — Backfill (0.5 day)

6. Write `BackfillCommand` runner.
7. Run against staging DB.
8. Inspect failure log; manually fix unparseable addresses.
9. Run against production DB.

### Phase 3 — Frontend map (1.5 days)

10. Install `leaflet`, `react-leaflet`, `@types/leaflet`.
11. Create `lib/maps/types.ts`, `lib/maps/providers/leaflet.tsx`, `lib/maps/index.ts`.
12. Create stub `lib/maps/providers/google.tsx` (throws `not yet implemented`).
13. Add `latitude` / `longitude` / `geocodingStatus` to `Property` type.
14. Build `<PropertyMap>` component with skeleton + empty state.
15. Insert into property detail page (between line 247 and 293).
16. Add i18n strings to `en.json` + `vi.json`.

### Phase 4 — Verification (0.5 day)

17. Test in browser: golden path (property with coords renders map), empty state (no coords), pending state (newly created property).
18. Verify scroll-wheel doesn't hijack page scroll.
19. Verify mobile layout (map should be full-width on small screens).
20. Lighthouse pass for the property page — Leaflet tiles shouldn't tank LCP.

**Total: ~4.5 days of engineering.**

---

## 18. Switching to Google Maps Later — Checklist

When the team is ready to switch, this is the entire change set:

### Backend

1. Add `google-maps-services-java` dependency to `property-service/pom.xml`.
2. Create `GoogleGeocodingProvider implements GeocodingProvider`.
3. Set env var: `MAP_GEOCODING_PROVIDER=google`, `GOOGLE_MAPS_API_KEY=...`.
4. Restart `property-service`.

### Frontend

5. Add `@react-google-maps/api` dependency.
6. Implement `GoogleMapsProvider` in `lib/maps/providers/google.tsx` (the stub from Phase 1).
7. Set env vars: `NEXT_PUBLIC_MAP_PROVIDER=google`, `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=...`.
8. Redeploy.

### What does **not** change

- `Property` DB schema, entity, DTO — coords are the same WGS84 numbers.
- `PropertyMap` component — it reads from `mapProvider`, doesn't know what provider it is.
- `PropertyService` — it calls `GeocodingProvider`, doesn't know the impl.
- i18n strings — the `attribution` string interpolates `mapProvider.name` so it updates automatically.

**This is the architectural promise of the abstraction.** If a future developer has to touch `PropertyMap.tsx` to switch providers, the abstraction has leaked and should be fixed.

---

## 19. Out of Scope

| Feature | Reason | Future PRP |
|---------|--------|------------|
| Manual lat/lng override / pin-drag in property editor | v1 keeps the editor flow unchanged | "Property location editor" |
| Map-based search (draw polygon, search in bounding box) | Requires search service + spatial index | "Property geo-search" |
| Nearby amenities overlay (schools, transit) | Needs Overpass / Places integration | "Property amenities" |
| Privacy fuzzing of coordinates | No demand yet | "Private listings" |
| Directions / routing | Native maps apps do this well via deep link | — |
| Heatmaps / clustering on listings page | Different page, different scope | "Listings map view" |

---

## 20. Success Metrics

| Metric | Target | How Measured |
|--------|--------|--------------|
| Properties with successful coords | ≥ 90% | `SELECT COUNT(*) FILTER (WHERE geocoding_status='SUCCESS') / COUNT(*) FROM properties` |
| Median geocoding latency (new properties) | ≤ 30s from create to SUCCESS | Kafka consumer lag + DB timestamps |
| Map render p95 latency on property page | ≤ 1.5s after page load | Frontend RUM / Web Vitals (LCP delta) |
| Geocoding failure rate | ≤ 5% | Count of `geocoding_status='FAILED'` / total |
| User engagement | Marker click-through ≥ 10% of property views | Frontend analytics event |

---

## Appendix A — Files Touched / Created

| Path | Change |
|------|--------|
| `property-service/src/main/resources/db/migration/V{n}__add_property_coordinates.sql` | New |
| `property-service/src/main/java/com/realestate/backend/entity/Property.java` | Modified |
| `property-service/src/main/java/com/realestate/backend/entity/GeocodingStatus.java` | New |
| `property-service/src/main/java/com/realestate/backend/dto/PropertyDTO.java` | Modified |
| `property-service/src/main/java/com/realestate/backend/service/PropertyService.java` | Modified |
| `property-service/src/main/java/com/realestate/backend/maps/GeocodingProvider.java` | New (interface) |
| `property-service/src/main/java/com/realestate/backend/maps/NominatimGeocodingProvider.java` | New |
| `property-service/src/main/java/com/realestate/backend/maps/BackfillCommand.java` | New |
| `property-service/src/main/resources/application.yml` | Add `app.maps.*` config |
| `frontend/package.json` | Add `leaflet`, `react-leaflet`, `@types/leaflet` |
| `frontend/lib/maps/types.ts` | New |
| `frontend/lib/maps/index.ts` | New |
| `frontend/lib/maps/providers/leaflet.tsx` | New |
| `frontend/lib/maps/providers/google.tsx` | New (stub) |
| `frontend/components/PropertyMap.tsx` | New |
| `frontend/types/api.ts` | Add 3 fields to `Property` |
| `frontend/app/properties/[id]/page.tsx` | Insert `<PropertyMap>` between line 247 and 293 |
| `frontend/messages/en.json` | Add `maps` section |
| `frontend/messages/vi.json` | Add `maps` section |
