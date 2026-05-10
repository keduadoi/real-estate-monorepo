# PRD — AI Natural-Language Property Search

**Status:** Draft v1 · **Owner:** TBD · **Last updated:** 2026-05-09

---

## 1. Summary

Today users find properties via a structured filter form (`/search`): pick city, type, status, price range, bedrooms, etc. This works but assumes the user can translate what they want into our schema. We want to add a **natural-language search bar** that accepts free-form Vietnamese or English (e.g. *"a house near the beach with 3 bedrooms, price cannot be over 5 billion VND"*) and returns the right properties.

**Approach (MVP):** use a *query parser* that converts the user's sentence into our existing `PropertySearchRequest` plus a small free-text "semantic hint", we run the existing search, and surface what was understood so the user can adjust. No vector DB, no embedding pipeline. The parser is **pluggable behind a config flag**: a fast deterministic **regex parser** (zero external dependencies, free, ~1 ms) and an **LLM parser** (Claude Haiku 4.5, broader coverage, ~1 s, ~$0.002/query). Operators can flip the mode per environment without touching code or redeploying clients. We get 80% of the value at ~5% of the cost/complexity of a full RAG system.

---

## 2. Goals & non-goals

### Goals
- Let users describe properties in plain language and get relevant results in **< 3 s p95**.
- Work for both **Vietnamese and English** (and code-switched mixes — common in VN real estate).
- Make the model's interpretation **visible and editable** ("we understood: 3 bedrooms, max 5B VND, coastal cities" → user can remove a chip).
- Stay within the existing platform shape: Spring Boot microservice, Postgres-backed property data, Kong gateway, NextAuth/JWT.
- Keep cost predictable: target **< $0.002 per query** at scale, hard daily ceiling per IP/user.

### Non-goals (this PRD)
- Conversational follow-ups ("now show only the cheaper ones") — single-turn only in MVP.
- Vector / semantic / RAG search — explicitly deferred to v2 (see §10).
- Voice input.
- Map-based / geospatial radius search — we don't have lat/lng on properties yet.
- Recommendations / personalization based on user history.

---

## 3. Users & user stories

**Primary persona:** a casual home-buyer who knows what they want in their own words but gives up halfway through filling 6 filter dropdowns.

| # | As a … | I want to … | So that … |
|---|--------|-------------|-----------|
| US-1 | renter | type *"căn hộ 2PN gần trung tâm Hà Nội dưới 20 triệu/tháng"* | I get a relevant rental list without picking each filter |
| US-2 | buyer | type *"a house near the beach with 3 bedrooms, max 5 billion VND"* | the system maps "near the beach" to coastal cities (Đà Nẵng, Nha Trang, Phú Quốc…) |
| US-3 | any user | see chips showing what the AI extracted (price, bedrooms, location) | I can verify the interpretation and tweak it |
| US-4 | any user | edit a chip or click "use structured search instead" | I'm never trapped if the AI mis-parses |
| US-5 | admin | see logs of NL queries + interpreted filters + result counts | I can tune prompts and spot mis-parses |

---

## 4. UX

### 4.1 Entry points
- **Buy / Rent / Search pages**: replace the existing `SearchBar` with a dual-mode bar:
  - default text input is now the **NL search**;
  - a small "Bộ lọc nâng cao" toggle reveals the existing structured form.
- **Home redirect (`/`)**: lands on `/buy` (current behavior); the AI bar is the first thing visible.

### 4.2 Flow

```
┌─────────────────────────────────────────────────────────────┐
│  🔍  Mô tả nhà bạn muốn tìm…                          [Tìm] │
└─────────────────────────────────────────────────────────────┘
              ↓ user submits
   POST /api/ai-search/parse  ─────────► ai-search-service
                                                  │
                                                  ▼
                                         Anthropic API
                                       (Claude Haiku 4.5
                                        + tool-use schema)
                                                  │
                                                  ▼
                                  PropertySearchRequest JSON
              ↓ frontend gets parsed filters
   POST /api/properties/search  ─────► property-service (existing)
              ↓
   Result page renders with:
     • interpreted-as chips (editable)
     • property grid
     • "Did we get this wrong? Use filter form" link
```

### 4.3 Result page additions
- **Interpreted chips row** above the grid:
  `[3 PN] [≤ 5 tỷ VND] [Vùng ven biển: Đà Nẵng, Nha Trang, Phú Quốc] [Loại: Nhà]`
  Each chip has an `×` to remove it; removing a chip re-runs the search.
- **Original query** echoed in small text: *"You searched: a house near the beach…"*
- **Confidence indicator** if any field is `low` confidence: small ⚠️ icon with hover tooltip "We weren't sure about this — feel free to remove."

### 4.4 Empty/error states
- Zero results: "Không tìm thấy kết quả. Thử nới lỏng giá hoặc khu vực." plus the chips so user can edit.
- LLM timeout/error: fall back to keyword search (treat the NL query as a `query` string in `PropertySearchRequest`) and show a non-blocking banner *"AI search is temporarily unavailable, showing keyword results."*
- Rate-limit: friendly message "Bạn đã tìm quá nhiều lần, thử lại sau X giây."

---

## 5. Architecture

### 5.1 New microservice: `ai-search-service`

Justification for separation (vs. adding to `property-service`):
- The Anthropic API key is a sensitive cross-cutting credential; isolating it limits blast radius.
- Different scaling profile (LLM calls are slow + bursty) — don't want it taxing property-service threads.
- Easy to swap models or providers later without touching property data code.
- Matches the existing pattern (price-service, news-service all live independently).

| Item | Value |
|---|---|
| Language / framework | Spring Boot 3 + Java 17 (matches platform) |
| Port | REST 8086 |
| Database | None initially. Optional Redis later for response cache. |
| Outbound | Anthropic API (`https://api.anthropic.com/v1/messages`) |
| Talks to | property-service via REST (`POST /api/properties/search`) for the actual lookup |
| Auth model | Header-trust (consistent with platform): `X-User-*` headers from frontend; rate-limit by user-id-or-ip |

### 5.2 Parser modes (config switch)

Two parser implementations live behind a common interface (`QueryParser`). The active one is chosen by config at service startup; clients see the same `POST /api/ai-search/parse` contract either way.

| Mode | Engine | Latency | Cost | Coverage | When to use |
|---|---|---|---|---|---|
| `regex` | In-process Java regex + keyword maps | ~1–5 ms | $0 | Narrow — handles the patterns we encode (price magnitudes, bed/bath counts, known city/district names, rent/sale keywords) | Local dev, CI, cost-sensitive envs, fallback when LLM is degraded |
| `llm`   | Claude Haiku 4.5 + tool use | ~600–1200 ms | ~$0.001–0.002 | Broad — paraphrases, code-switching, fuzzy intents ("near the beach", "modern", "yên tĩnh") | Default for staging / prod |

**Config keys** (Spring `application.yml`, env-overridable):

```yaml
ai-search:
  parser:
    mode: llm            # llm | regex          (env: AI_SEARCH_PARSER_MODE)
    fallback-on-error: regex   # regex | none   (env: AI_SEARCH_PARSER_FALLBACK)
```

- `mode=regex` skips the Anthropic call entirely — no API key needed, no network egress, no per-query cost.
- `mode=llm` with `fallback-on-error=regex` runs the LLM first and silently falls through to regex on timeout / 5xx / quota errors (preserves the keyword-fallback UX from §4.4 without needing a separate path).
- Switching modes requires only a service restart (env var change). No client changes; no schema changes.

**Response signaling.** The `parse` response includes `"parserMode": "regex" | "llm"` so the frontend can show a small `⚡ regex` / `✨ AI` badge for transparency and so analytics can split metrics by mode.

The regex parser is described in §7.1.1; the LLM parser is described in §5.3 onward.

### 5.3 LLM choice

**Recommendation: Claude Haiku 4.5** (`claude-haiku-4-5-20251001`).

| Why | |
|---|---|
| Latency | ~600–1200 ms typical for ~500-token prompts — fits our 3 s budget |
| Cost | Roughly $0.001–0.002 per parse at our prompt size |
| Multilingual | Strong Vietnamese; handles code-switching well |
| Tool use | Reliable structured output via tool definitions — no JSON-mode flakiness |
| Cutoff | Recent enough for VN city/district names |

Plan to upgrade to Sonnet only if Haiku misparses too often (track in §11 metrics).

### 5.4 Why API key, not subscription

Confirmed earlier in the program: third-party programmatic callers must use the Claude API. `ai-search-service` is a server program calling Anthropic from a backend — straightforward API-key flow. Key stored in Spring `@ConfigurationProperties` from env var `ANTHROPIC_API_KEY`, supplied via Docker compose / k8s secret. **Never sent to the browser.**

### 5.5 Sequence diagram

```
Browser  ─POST /api/ai-search/parse─►  Kong  ─►  ai-search-service
                                                 │
                                                 ├─ rate-limit check (per user/IP)
                                                 ├─ cache lookup (hash of query+lang+mode)
                                                 │
                                                 ├── mode == regex ──► RegexQueryParser
                                                 │                         │ (~1–5 ms)
                                                 │                         ▼
                                                 │                  parsed filters
                                                 │
                                                 └── mode == llm   ──► Anthropic /v1/messages
                                                                       (Claude Haiku 4.5 + tool)
                                                                          │
                                                                          ▼
                                                                  Validate parsed JSON
                                                 │                        │
                                                 ◄────────────────────────┘
                                                 │  (on LLM error: optional fallback to regex)
                                                 ▼
                                          parsed filters + chips + parserMode
Browser  ◄───────────────────────────────────────┘

Browser  ─POST /api/properties/search (existing)─► property-service
Browser  ◄──── PageResponse<PropertyDTO> ─────────
```

We *don't* hide the property-search call inside ai-search-service. The frontend orchestrates both, so:
- Caching the parse step is independent of pagination/sorting.
- Removing a chip just re-runs the cheap search call without re-hitting the LLM.

---

## 6. API design

### 6.1 New endpoint

**`POST /api/ai-search/parse`**

Request:
```json
{
  "query": "a house near the beach with 3 bedrooms, price cannot be over 5 billion vnd",
  "locale": "vi-VN"
}
```

Response (200):
```json
{
  "originalQuery": "a house near the beach with 3 bedrooms, ...",
  "filters": {
    "propertyType": "HOUSE",
    "status": "FOR_SALE",
    "bedrooms": 3,
    "maxPrice": 5000000000,
    "cities": ["Đà Nẵng", "Nha Trang", "Phú Quốc", "Vũng Tàu"],
    "freeText": "near the beach"
  },
  "chips": [
    { "id": "type",     "label": "Loại: Nhà",                   "field": "propertyType", "value": "HOUSE",      "confidence": "high" },
    { "id": "beds",     "label": "3 phòng ngủ",                 "field": "bedrooms",     "value": 3,            "confidence": "high" },
    { "id": "max",      "label": "≤ 5 tỷ VND",                  "field": "maxPrice",     "value": 5000000000,   "confidence": "high" },
    { "id": "loc",      "label": "Vùng ven biển: 4 thành phố",  "field": "cities",       "value": [...],        "confidence": "medium" }
  ],
  "warnings": [],
  "parserMode": "llm",
  "parserLatencyMs": 842,
  "cacheHit": false
}
```

`parserMode` echoes the parser that produced this response (`"llm"` or `"regex"`). `parserLatencyMs` is the end-to-end parse time regardless of mode (replaces the LLM-specific `modelLatencyMs`).

Response (4xx/5xx): standard `ErrorResponse` (matches news-service pattern).

### 6.2 Why both `cities` *and* `freeText`
Some intents like "near the beach" map cleanly to a city set — return both so the frontend can choose: do an SQL `city IN (…)` filter (fast, exact) and *also* show the user's exact phrase as a fallback chip ("near the beach") that could be relaxed.

### 6.3 Tool-use schema (Anthropic-side)

We define a single tool the model is forced to call, schema = our parsed-filter shape. This is the most reliable way to get strict JSON.

```json
{
  "name": "extract_property_search",
  "description": "Extract structured property search filters from a Vietnamese or English real-estate query.",
  "input_schema": {
    "type": "object",
    "properties": {
      "propertyType":  { "type": "string", "enum": ["HOUSE","APARTMENT","VILLA","TOWNHOUSE","UNKNOWN"] },
      "status":        { "type": "string", "enum": ["FOR_SALE","FOR_RENT","UNKNOWN"] },
      "bedrooms":      { "type": "integer", "minimum": 0, "maximum": 20 },
      "bathrooms":     { "type": "integer", "minimum": 0, "maximum": 20 },
      "minPrice":      { "type": "number" },
      "maxPrice":      { "type": "number" },
      "minArea":       { "type": "number" },
      "maxArea":       { "type": "number" },
      "cities":        { "type": "array", "items": { "type": "string" } },
      "districts":     { "type": "array", "items": { "type": "string" } },
      "freeText":      { "type": "string" },
      "warnings":      { "type": "array", "items": { "type": "string" } }
    },
    "required": ["warnings"]
  }
}
```

System prompt highlights (full prompt in service repo):
- Currency normalization: `5 tỷ`, `5 billion VND`, `5,000,000,000 đ`, `5B` → `5000000000`.
- Rent vs. sale heuristics: `/tháng`, `cho thuê`, `rent`, `monthly` → `FOR_RENT`.
- Coastal/center mappings: provide a list (`coastal_cities`, `central_business_districts`) the model can fall back to.
- If unsure, leave the field out and add a string to `warnings`.

### 6.4 Prompt caching

Use Anthropic prompt caching on the system prompt + tool definition (they are stable). Saves ~70% on input tokens after the first call in a 5-minute window.

---

## 7. Backend changes (per service)

### 7.1 `ai-search-service` (new)
- `pom.xml` mirrors news-service (no JPA, no Flyway — stateless).
- Dependency on official Anthropic Java SDK (or plain WebClient — small surface, but SDK gives prompt-caching helpers). Optional at runtime: not required when `mode=regex`.
- `QueryParser` interface with two implementations: `RegexQueryParser` (§7.1.1) and `LlmQueryParser` (wraps `AnthropicClient.runTool(...)`).
- `AiSearchController.parse(...)` → `AiSearchService.parse(...)` → `QueryParser` (selected by `ai-search.parser.mode`).
- `ParserModeConfig` — Spring `@ConfigurationProperties("ai-search.parser")` exposing `mode` and `fallbackOnError`. Reads `AI_SEARCH_PARSER_MODE` / `AI_SEARCH_PARSER_FALLBACK` env vars. Both parsers are wired as beans; the active one is chosen by name.
- `RateLimitFilter` — bucket per `X-User-Id` (fallback to `X-Forwarded-For`). Limits in §9. Only applied when `mode=llm` (regex is free; rate-limit becomes a thin abuse-protection layer at higher thresholds).
- `ResponseCache` — Caffeine in-process (1k entries, 10 min TTL). Cache key includes `parserMode` so flipping the switch doesn't serve stale cross-mode results. Move to Redis when we have multi-replica.

#### 7.1.1 `RegexQueryParser`

Pure Java, no I/O, no external calls. Uses ordered regex passes against a normalized query (lowercased, accent-folded for keyword matching only — original kept for `freeText`).

| Pass | Examples it catches | Output field |
|---|---|---|
| Currency / price | `dưới 5 tỷ`, `≤ 5 billion vnd`, `5,000,000,000 đ`, `under 3B`, `tu 2 den 4 ty` | `minPrice` / `maxPrice` |
| Per-month rent signal | `/tháng`, `mỗi tháng`, `cho thuê`, `rent`, `monthly` | `status=FOR_RENT` |
| Sale signal | `bán`, `for sale` (default if neither rent nor sale matches) | `status=FOR_SALE` |
| Bed/bath count | `3 PN`, `3 phòng ngủ`, `3 bedrooms`, `3 BR`, `2 WC`, `2 bathrooms` | `bedrooms` / `bathrooms` |
| Area | `80 m2`, `80m²`, `80 sqm` | `minArea` / `maxArea` |
| Property type | `nhà phố` → TOWNHOUSE, `căn hộ`/`apartment` → APARTMENT, `biệt thự`/`villa` → VILLA, `nhà`/`house` → HOUSE | `propertyType` |
| Location | Match against curated `cities.yml` and `districts.yml` (same lists §6.3 system prompt uses); also coastal/center keyword sets | `cities` / `districts` |
| Residual | Everything not consumed by a pass above | `freeText` |

Anything ambiguous (e.g. price without "min/max/under/over" qualifier) goes into `warnings` exactly like the LLM path. Unit tests cover the golden set in Appendix A — regex must pass at least the first three rows; subjective phrasings (`"modern"`, `"có view đẹp"`) are expected to fall through to `freeText` only.

When `mode=regex` is active, the chip `confidence` field is `"high"` for any field a regex matched and `"low"` for `freeText`-only output. No `medium` tier — keeps the implementation simple and the user's expectations calibrated.

### 7.2 `property-service`
- **No code changes required for MVP.** We reuse `POST /api/properties/search` exactly as-is.
- Future-only (out of scope here): when filtering by a list of cities, we currently only accept a single `city`. Either extend the search request to accept `cities: List<String>` or have the frontend make N parallel calls. Recommend extending the request — small change.

### 7.3 Kong
Add a public route in `kong/config/kong-local.yaml`:
```yaml
- name: ai-search-public
  service: ai-search-service
  paths: [ /api/ai-search ]
  methods: [ POST, OPTIONS ]
  strip_path: false
```
And a service block at port 8086. Apply rate-limiting plugin at the Kong layer too as defense-in-depth.

### 7.4 `auth-service`
No changes. AI search is available to anonymous users (read-only intent).

---

## 8. Frontend changes

### 8.1 Components
- **New:** `components/AiSearchBar.tsx` — text area + submit; calls `aiSearchApi.parse`, then `propertyApi.search`, manages loading/error states. Lives at the top of `/buy`, `/rent`, and `/search`.
- **New:** `components/InterpretedChips.tsx` — chips with × removal; emits change events.
- **New:** `lib/api/aiSearchApi.ts` — single method `parse(query, locale)`.
- **New types** in `types/api.ts`: `AiSearchParseResponse`, `AiSearchChip`.
- **Minor edits** to `app/buy/page.tsx`, `app/rent/page.tsx`, `app/search/page.tsx` to mount `AiSearchBar` above the existing grid; preserve current SSR behavior.

### 8.2 i18n
The codebase now uses `next-intl` (per Header.tsx, buy/rent pages). New keys to add to messages:
```
aiSearch.placeholder      "Mô tả nhà bạn muốn tìm…" / "Describe what you're looking for…"
aiSearch.submit           "Tìm"     / "Search"
aiSearch.interpretedAs    "Hiểu là:" / "Interpreted as:"
aiSearch.lowConfidence    "Có thể chưa đúng — bấm × nếu sai"
aiSearch.fallbackBanner   "AI tạm không khả dụng, đang dùng tìm kiếm thường"
aiSearch.rateLimited      "Bạn đã tìm quá nhiều — thử lại sau {seconds}s"
```
The system prompt sent to Anthropic should include the user's locale so explanatory chip labels come back appropriately localized — **but** to keep server logic simple and language-agnostic, the LLM returns *machine codes / values* and the frontend formats labels via `next-intl`. The model does NOT generate UI strings.

### 8.3 Navigation
Optional in v1: add a small `✨ AI search` badge next to the existing search icon. Skip a dedicated `/ai-search` route — the bar lives on the existing pages.

---

## 9. Non-functional requirements

| Concern | `mode=llm` target | `mode=regex` target |
|---|---|---|
| Latency p50 (parse) | ≤ 1.2 s | ≤ 10 ms |
| Latency p95 end-to-end | ≤ 3 s | ≤ 250 ms |
| Availability | 99.5% (degrades to keyword search on LLM failure) | 99.9% (in-process, no external dep) |
| Cost ceiling | ≤ $50 / day in MVP (alarm at $30) | $0 |
| Per-user rate | 30 NL queries / 5 min, 200 / day | 120 / 5 min, 1000 / day |
| Per-IP rate (anon) | 10 queries / 5 min, 50 / day | 60 / 5 min, 300 / day |
| Cache hit ratio | Aim ≥ 25% after first week | N/A (parse is already cheaper than cache lookup) |
| Logged data | query text, parsed filters, result count, latency, parserMode. **No PII beyond user-id.** | same |

### Security
- API key only readable by `ai-search-service` pod / container (env var, not committed).
- Frontend never sees the key.
- Validate the LLM's output server-side: enum membership, integer ranges (`bedrooms ≤ 20`, `maxPrice ≤ 1e15`), city names against a known list — drop unknown cities into `warnings` instead of trusting blindly.
- Strip control chars / extreme lengths from the user query before sending (prompt-injection mitigation; we're not executing untrusted output, but still).
- Log queries for safety review (have a kill-switch to disable the endpoint via env var).

---

## 10. Out-of-scope / v2 candidates

These are real but explicitly deferred so v1 ships in 2 weeks not 8.

1. **Embeddings + pgvector**: index property `description + features + city` with `text-embedding-3-large` (or Voyage). Cosine-rank within hard-filtered set. Big lift on subjective queries ("modern", "cozy", "có view đẹp"). Defer until we see the Haiku-only version's miss patterns.
2. **Conversational follow-ups**: stash the last `filters` per session and let the next query refine ("now show me cheaper ones"). Needs a small session/context cache.
3. **Map / geospatial**: requires lat/lng on properties (schema migration + geocoding pipeline).
4. **Saved searches & alerts**: "tell me when something matching this query is listed."
5. **Price-rate awareness**: cross-reference the price-service's history to flag overpriced/underpriced results inline.
6. **"Why this match"** explanation under each card ("3 BR ✓  beach city ✓  4.2B VND ≤ 5B ✓"). Cheap to add once chips ship; left for v2 only because it changes card layout.

---

## 11. Success metrics (review @ 4 weeks post-launch)

| Metric | Definition | Target |
|---|---|---|
| **Adoption** | % of search-page sessions that submit at least one NL query | ≥ 30% |
| **Successful parse rate** | % of parses where ≥ 1 chip is returned with `confidence ≥ medium` | ≥ 90% |
| **Click-through rate** | % of NL searches where user clicks at least one property | ≥ 25% (vs. ≥ 35% on structured search — match within 10 pts) |
| **Chip-edit rate** | % of NL searches where user removes/edits ≥ 1 chip before browsing | ≤ 25% (high = bad parse) |
| **Fallback rate** | % of NL searches falling back to keyword due to LLM error | ≤ 1% |
| **Cost per search** | total Anthropic spend / NL queries | ≤ $0.002 |

Mis-parse review process: weekly, dump 50 random `chip-edit ≥ 1` queries → categorize → tune system prompt or city list.

---

## 12. Rollout plan

| Week | Milestone |
|---|---|
| 1 | Skeleton ai-search-service + `QueryParser` interface + `RegexQueryParser` shipping behind `mode=regex`; Kong route; AiSearchBar UI behind feature flag (`NEXT_PUBLIC_AI_SEARCH=1`). Regex alone is enough to dogfood the UX end-to-end with zero external cost. |
| 1 | Anthropic SDK integration, system prompt + tool definition, golden-set tests (20 known queries → expected filters) for both parsers — same suite, both modes must pass the deterministic rows. |
| 2 | Caching + rate-limiting, cost dashboards; wire `fallback-on-error=regex` so LLM degradation is invisible to users |
| 2 | Internal dogfood: enable `mode=llm` for ROLE_ADMIN only; everyone else stays on regex |
| 3 | Public beta: flip default to `mode=llm` site-wide; keep "Use filter form instead" link visible; collect metrics split by `parserMode` |
| 4 | Review metrics; decide whether to invest in v2 (embeddings), and whether regex stays as a permanent low-cost / dev / fallback mode (recommended yes). |

---

## 13. Open questions

1. **Scope of "near the beach" mapping** — do we ship a curated coastal-city list, or rely entirely on the model? *Recommend curated list (15–20 entries) checked into config; the model returns names, we filter to known ones.*
2. **Anonymous use** — let logged-out users hit the AI bar? *Recommend yes, with stricter per-IP limits, since most browsers won't be logged in.*
3. **Caching semantics** — cache key normalization (lowercase, trim, strip punctuation, locale) — exact vs. fuzzy. *Recommend exact match only in MVP; revisit if cache hit rate is low.*
4. **Cities filter on property-service** — extend `PropertySearchRequest.cities: List<String>` now (small migration) or have the frontend run N parallel searches and merge? *Recommend extending — cleaner, fewer round-trips.*
5. **Where does the API key live in dev vs. prod** — Docker secret, k8s secret, or just env from a `.env` file? *Recommend env from `.env.local` for dev (gitignored), k8s Secret for prod.*
6. **Audit/log retention** — how long do we keep raw NL queries? *Recommend 90 days, then aggregate-only metrics.*
7. **Default parser mode per environment** — local/CI: `regex` (no key needed, deterministic tests). Staging/prod: `llm` with `fallback-on-error=regex`. *Confirm before shipping. Also: do we expose the mode to end-users (e.g. a "Faster, less smart" toggle) or keep it operator-only? Recommend operator-only in v1; revisit if regex quality is good enough to be a user-visible "free tier".*

---

## Appendix A — Example parses (golden set seed)

| Query | Expected key fields |
|---|---|
| `a house near the beach with 3 bedrooms, price cannot be over 5 billion vnd` | type=HOUSE, beds=3, maxPrice=5e9, cities=[coastal set], status=FOR_SALE (default) |
| `căn hộ 2PN gần trung tâm Hà Nội dưới 3 tỷ` | type=APARTMENT, beds=2, maxPrice=3e9, cities=[Hà Nội], status=FOR_SALE |
| `nhà phố Quận 7 cho thuê dưới 20 triệu/tháng` | type=TOWNHOUSE, status=FOR_RENT, maxPrice=2e7, districts=[Quận 7] |
| `biệt thự view biển Đà Nẵng, 4 phòng ngủ, dưới 30 tỷ` | type=VILLA, beds=4, maxPrice=3e10, cities=[Đà Nẵng], freeText="view biển" |
| `something cheap to rent` | status=FOR_RENT, sortBy=price_asc, warnings=["price not specified, sorting by lowest"] |

## Appendix B — File touchpoints (estimated)

```
NEW  ai-search-service/                     (mirrors news-service skeleton, ~10 files)
NEW  frontend/components/AiSearchBar.tsx
NEW  frontend/components/InterpretedChips.tsx
NEW  frontend/lib/api/aiSearchApi.ts
EDIT frontend/types/api.ts                  (+ AiSearch types)
EDIT frontend/app/buy/page.tsx              (mount AiSearchBar)
EDIT frontend/app/rent/page.tsx
EDIT frontend/app/search/page.tsx
EDIT frontend/i18n/messages/vi.json         (+ aiSearch.* keys)
EDIT frontend/i18n/messages/en.json
EDIT kong/config/kong-local.yaml            (+ ai-search-service service & route)
EDIT scripts/start-all-services.sh          (+ start ai-search-service step)
EDIT scripts/stop-all-services.sh
EDIT scripts/status-all-services.sh
EDIT property-service: PropertySearchRequest accepts cities: List<String>  (small)
```
