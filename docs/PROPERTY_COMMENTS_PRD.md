# PRD — Property Comments

**Status:** Draft v1 · **Owner:** TBD · **Last updated:** 2026-05-10

---

## 1. Summary

Property listing pages (`/properties/{id}`) today are a one-way broadcast: the owner posts, the world reads. Buyers and renters routinely have questions ("còn không?", "bao gồm phí dịch vụ?", "có thương lượng được không?") that today happen out-of-band on Zalo/phone, leaving no public record for the next 100 visitors with the same question. We want a **comment thread on each property** where anyone — anonymous visitors and logged-in users alike — can ask, reply, and read prior discussion.

**Approach (MVP):** a new stateless-ish microservice `comment-service` (port 8087, own Postgres DB) that owns comment data; one row per comment with optional `parent_id` for 1-level replies; anonymous posting gated by **display name + lightweight captcha** (no email verification); per-IP and per-property rate limits; admin moderation queue. Likes (👍 only) and reporting included. No threading beyond 1 level, no markdown, no real-time push, no email notifications — those are v2.

---

## 2. Goals & non-goals

### Goals
- Let logged-in **and** anonymous users post comments on any property listing in **< 1 s p95**.
- Make the comment composer feel as light as a Disqus / Facebook comment box: name + captcha + textarea, that's it.
- Support 1-level replies so a poster can answer follow-up questions inline.
- Make moderation easy: admin can hide any comment with one click; soft-delete only (recoverable for 30 days).
- Keep abuse cost low: per-IP / per-property rate limits, captcha for anon writes, honeypot, profanity filter.
- Match the existing platform shape: Spring Boot microservice, Postgres-backed, Kong gateway routing, header-trust auth.

### Non-goals (this PRD)
- Real-time / live updates (websockets, SSE) — refresh-on-submit + manual reload only.
- Email or push notifications when someone replies to you.
- Mentions (`@user`), markdown, or image attachments — plain text + line breaks only.
- Threading deeper than 1 level (replies-to-replies). UI flattens those into the same level if attempted.
- Comment search across the entire site.
- Migration of existing Zalo / phone conversations — comments start empty.
- Comments on news articles or social-feed posts (this service could later be reused, but not in scope here).

---

## 3. Users & user stories

| # | As a … | I want to … | So that … |
|---|--------|-------------|-----------|
| US-1 | anonymous visitor | type a name and ask "còn không?" without creating an account | I can get an answer in 30 seconds, not 30 minutes |
| US-2 | property owner (logged in) | reply to a question on my listing | the next 100 visitors see the answer too |
| US-3 | logged-in user | edit a typo in my own comment within 15 minutes of posting | I'm not stuck with embarrassing typos |
| US-4 | logged-in user | delete my own comment | I can retract questions I no longer want public |
| US-5 | any user | like (👍) a comment to signal "+1, I have the same question" | the owner can prioritize popular questions |
| US-6 | any user | report a spam / abusive comment | admin can review and hide it |
| US-7 | admin | see a moderation queue of reported comments | I can clean up spam in one place |
| US-8 | admin | hide any comment with one click (soft delete) | bad content disappears for users immediately and is recoverable for 30 days |

Out of scope users: SEO crawlers (we'll render comments server-side so they're indexable, but no special treatment), API consumers other than our own frontend.

---

## 4. UX

### 4.1 Placement
On `/properties/{id}` below the price-history section and above the related-properties grid. Section is collapsed-by-default to a teaser **"💬 N comments — show"** if `commentCount > 5`; otherwise renders inline.

### 4.2 Composer
```
┌─────────────────────────────────────────────────────────────┐
│  [👤] You're commenting as Anonymous                        │
│  ┌─ Display name ────────────────────┐                     │
│  │ Tên của bạn                       │                     │
│  └───────────────────────────────────┘                     │
│  ┌─ Comment ─────────────────────────────────────────────┐ │
│  │ Viết bình luận của bạn…                              │ │
│  │                                                       │ │
│  └───────────────────────────────────────────────────────┘ │
│  [ I'm not a robot — 3 + 4 = ___ ]   [Hủy] [Gửi bình luận] │
└─────────────────────────────────────────────────────────────┘
```

- **Logged-in users** skip the name field — name pulled from session, avatar from Gravatar (if email known) or initial chip.
- **Anon** users see the name field (required, 1–50 chars, no leading/trailing whitespace, no URLs in the name).
- **Captcha** is a simple math problem (`a + b`, `a × b` for 1 ≤ a,b ≤ 9) rendered as text + input. Server validates the challenge id + answer. Avoids an external dep for v1; can swap to hCaptcha if abuse warrants.
- **Honeypot**: a hidden `website` field — if filled, silently 200 and discard.
- **Submit** disabled when textarea is empty or > 1000 chars; counter shown at 800.

### 4.3 Comment item
```
┌─────────────────────────────────────────────────────────────┐
│  [TN] Tên Người · 2 giờ trước · Khách                       │
│       Còn không bạn? Mình quan tâm căn này.                 │
│       👍 3   ↩ Trả lời   ⋯                                   │
│                                                              │
│       └─ [HD] Hà (chủ tin) · 1 giờ trước                     │
│            Còn nhé. Bạn liên hệ Zalo trong mô tả.            │
│            👍 1   ⋯                                           │
└─────────────────────────────────────────────────────────────┘
```

- **Header line**: avatar (initial chip) + display name + relative timestamp + role badge (`Khách` for anon, `Chủ tin` if commenter == property.userId, `Quản trị` for admin).
- **Body**: plain text, `\n` → `<br>`. URLs are auto-linked (rel="ugc nofollow noopener"). No HTML, no markdown.
- **Actions**: 👍 like, ↩ reply (top-level only), ⋯ menu with Edit / Delete / Report depending on permissions.
- **Replies** are indented 32px and never have their own reply button — clicking reply on a reply targets the parent. (Keeps depth = 1.)
- **Hidden** comments (admin-hidden) render as a stub: *"This comment has been hidden by a moderator"* — preserves the thread structure but not the content.

### 4.4 List ordering & paging
- **Top-level comments**: newest-first by default (`ORDER BY created_at DESC`).
- **Replies under a parent**: oldest-first (preserves conversational flow).
- **Pagination**: 10 top-level comments per page; "Load more" button (no infinite scroll). Replies fully expand for now (small N expected).
- Future-only: a "popular" sort using `like_count + 0.5 * reply_count`.

### 4.5 Empty / error / rate-limit states
- **Empty**: *"Chưa có bình luận nào — hãy là người đầu tiên hỏi đáp về tin này."*
- **Submit error** (validation, captcha): inline red text under composer, doesn't clear the textarea.
- **Rate-limit hit**: friendly *"Bạn bình luận quá nhiều, thử lại sau {seconds} giây."*
- **Service down**: composer disabled, banner *"Bình luận tạm không khả dụng."*; existing comments still render (cached on the property page).

---

## 5. Architecture

### 5.1 New microservice: `comment-service`

| Item | Value |
|---|---|
| Language / framework | Spring Boot 3 + Java 17 (matches platform) |
| Port | REST 8087 |
| Database | New Postgres `commentsdb` on port 5437 |
| Migrations | Flyway (`db/migration/V1__create_comment_tables.sql`) |
| Outbound | None initially. Future: Kafka topic `comment.events` for notifications. |
| Talks to | Property-service (validates `propertyId` exists on POST — single GET; cached in Caffeine for 60 s) |
| Auth model | Header-trust: `X-User-Id` / `X-User-Roles` from Kong; absent = anonymous |

Justification for separation (vs. extending property-service or post-service):
- **Different scaling profile**: comment writes are bursty (a popular listing can spike to 50 RPS during VN evening hours); we don't want that contending with property CRUD.
- **Different abuse profile**: anon writes need their own rate-limit/captcha budget; isolating them keeps spam from blowing the property-service rate-limit budget.
- **Different ops cadence**: moderation tooling will iterate faster than property-service, which holds the canonical real-estate data.
- **Reusability**: same service can later host news / feed comments by adding a `subject_type` column (kept narrow to `PROPERTY` in v1 to avoid scope creep).
- Matches the existing pattern (price-service, news-service, ai-search-service all live independently).

### 5.2 Sequence diagram

```
Browser  ──GET /api/properties/{id}/comments─►  Kong  ──►  comment-service
                                                              │
                                                              ├─ rate-limit check (per IP)
                                                              ├─ DB SELECT
                                                              ▼
                                                        PageResponse<CommentDTO>
Browser  ◄──────── JSON ──────────────────────────────────────┘

Browser  ──POST /api/properties/{id}/comments─►  Kong  ──►  comment-service
                                                              │
                                                              ├─ rate-limit (per IP / per property)
                                                              ├─ captcha verify (anon only)
                                                              ├─ honeypot check
                                                              ├─ profanity filter (warn-only)
                                                              ├─ existence check (property-service GET, cached)
                                                              ├─ INSERT row
                                                              ▼
                                                        CommentDTO
Browser  ◄────────── JSON ────────────────────────────────────┘
```

The browser orchestrates one round-trip per action; comments-service never reaches into property-service for read paths.

### 5.3 Identity model

| Visitor type | Identity stored | Display |
|---|---|---|
| Logged-in (`X-User-Id` present) | `user_id` (UUID), nullable `guest_name` | `session.user.name`, avatar from gravatar/initial |
| Anonymous | `guest_name` (1–50 chars), `guest_email_hash` (optional, MD5 for gravatar), `ip_hash` (HMAC-SHA256 of IP, key in env) | guest_name, "Khách" badge |

Privacy:
- Raw IP **never** stored; only `HMAC(ip, server_secret)` for rate-limit lookups. Hash key rotates yearly (kills lookback ability).
- Raw email **never** stored. We compute `gravatar_hash = MD5(lowercase(trim(email)))` client-side and only the hash hits our DB.
- `user_agent` is truncated to 200 chars (drops fingerprinting tail) and used solely for abuse review.

---

## 6. API design

### 6.1 Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/properties/{propertyId}/comments?page=0&size=10` | none | List top-level comments + N replies each |
| GET | `/api/comments/{id}/replies?page=0&size=20` | none | Paginate replies if a parent has > 5 |
| POST | `/api/properties/{propertyId}/comments` | none (anon allowed) | Create top-level comment |
| POST | `/api/comments/{id}/reply` | none | Create reply (server enforces depth ≤ 1) |
| PUT | `/api/comments/{id}` | JWT, owner only | Edit body; window: 15 min after create |
| DELETE | `/api/comments/{id}` | JWT, owner OR admin | Soft-delete (sets `hidden_at = now`) |
| POST | `/api/comments/{id}/like` | none | Toggle like (per identity = user_id or ip_hash) |
| POST | `/api/comments/{id}/report` | none | Submit a report (max 1 per identity per comment) |
| GET | `/api/admin/comments?status=reported&page=0` | JWT + ROLE_ADMIN | Moderation queue |
| POST | `/api/admin/comments/{id}/hide` | JWT + ROLE_ADMIN | Hide with reason |
| POST | `/api/admin/comments/{id}/restore` | JWT + ROLE_ADMIN | Restore a hidden comment |
| GET | `/api/captcha` | none | Get a fresh challenge (id + question) |

### 6.2 GET response shape

```json
{
  "data": [
    {
      "id": 1042,
      "propertyId": 17,
      "parentId": null,
      "userId": null,
      "displayName": "Hà",
      "gravatarHash": null,
      "isOwnerOfProperty": false,
      "isAdmin": false,
      "body": "Còn không bạn?\nMình quan tâm.",
      "likeCount": 3,
      "likedByCurrent": false,
      "replyCount": 2,
      "replies": [
        { "id": 1043, "parentId": 1042, "userId": "uuid…", "displayName": "Hà (chủ tin)", "isOwnerOfProperty": true, "body": "Còn bạn ơi.", "likeCount": 1, "createdAt": "2026-05-10T08:30:00Z" }
      ],
      "createdAt": "2026-05-10T07:30:00Z",
      "updatedAt": null,
      "editable": false
    }
  ],
  "total": 14,
  "page": 0,
  "perPage": 10,
  "totalPages": 2
}
```

`editable=true` when the current viewer is the comment author and `now - createdAt < 15 min` and the comment isn't hidden.

### 6.3 POST request shape (anon)

```json
{
  "body": "Căn này còn không bạn?",
  "displayName": "Tên người dùng",
  "gravatarHash": "0bc83cb571cd1c50ba6f3e8a78ef1346",
  "captchaId": "c-7f2e…",
  "captchaAnswer": "12",
  "website": ""
}
```

- `website`: honeypot. Non-empty → silent 200, no insert.
- `gravatarHash`: optional client-computed; server validates `^[a-f0-9]{32}$`.
- For logged-in users, `displayName` and `gravatarHash` are ignored; trusted from JWT claims.

### 6.4 Errors

Standard `ErrorResponse` shape (matches news-service):

| Code | HTTP | When |
|---|---|---|
| `RATE_LIMITED` | 429 | per-IP or per-property bucket exhausted; includes `Retry-After` header |
| `CAPTCHA_FAILED` | 400 | wrong answer or expired challenge id |
| `BODY_TOO_LONG` | 400 | > 1000 chars |
| `BODY_EMPTY` | 400 | trimmed body is empty |
| `PROPERTY_NOT_FOUND` | 404 | upstream property-service GET 404s |
| `EDIT_WINDOW_EXPIRED` | 403 | PUT after 15-min window |
| `FORBIDDEN` | 403 | edit/delete on someone else's comment without admin role |
| `MAX_DEPTH_EXCEEDED` | 400 | reply on a comment that is itself a reply |

---

## 7. Backend changes (per service)

### 7.1 `comment-service` (new)
- `pom.xml` mirrors news-service (Spring Web + Data JPA + Validation + Flyway + Postgres + Caffeine + Actuator).
- `entity/Comment` (JPA), `repository/CommentRepository` (Spring Data JPA + custom counts).
- `controller/CommentController` (public), `controller/AdminCommentController` (ROLE_ADMIN guard).
- `service/CommentService` — orchestration: validation, captcha verify, profanity filter, rate-limit check, persist.
- `service/CaptchaService` — Caffeine-backed challenge store, 5-min TTL, single-use.
- `service/ProfanityFilter` — small inline Vietnamese + English wordlist; hits result in `warning=profanity_suspected` flag, not auto-block (false-positive cost is high in VN).
- `service/PropertyExistenceClient` — WebClient call to `property-service/api/properties/{id}`; result cached 60 s in Caffeine.
- `security/UserContextFilter` — same shape as property-service (reads `X-User-*` headers from Kong).
- `RateLimitFilter` — Bucket4j or hand-rolled Caffeine counter:
  - **Per IP**: 5 writes / 5 min, 30 / day.
  - **Per property**: 20 writes / min (cap a single hot listing).
  - **Per logged-in user**: 30 writes / 5 min, 200 / day (looser than IP).

### 7.2 `property-service`
- **No code changes required for MVP.** comment-service reads existence via the public GET endpoint, no new contract.
- Future-only (out of scope here): expose a `commentCount` field on `Property` for the listing card. Either denormalize in property-service via a Kafka subscription on `comment.events`, or compute on the frontend by batch-querying `/api/comments/counts?ids=…` (recommended — simpler, eventually consistent).

### 7.3 `auth-service`
- No changes. JWT payload already carries the user UUID, name, and roles.

### 7.4 Kong (`kong/config/kong-local.yaml`)
Add a service block at port 8087 + routes:
```yaml
services:
  - name: comment-service
    url: http://host.docker.internal:8087
    ...

routes:
  - name: comments-public
    service: comment-service
    paths: [ /api/properties, /api/comments, /api/captcha ]
    methods: [ GET, POST, PUT, DELETE, OPTIONS ]
    strip_path: false
  - name: comments-admin
    service: comment-service
    paths: [ /api/admin/comments ]
    strip_path: false
```
Plus a global `rate-limiting` plugin with policy=local for defense-in-depth.

---

## 8. Frontend changes

### 8.1 Components
- **New**: `components/PropertyComments.tsx` — server-side data fetch + client wrapper.
- **New**: `components/CommentComposer.tsx` — textarea + name input (anon) + captcha + honeypot.
- **New**: `components/CommentItem.tsx` — single comment with reply / like / edit / delete buttons.
- **New**: `components/CommentList.tsx` — list with "Load more" pagination.
- **New**: `lib/api/commentApi.ts` — REST client with `list`, `create`, `reply`, `update`, `remove`, `like`, `report`, `getCaptcha`.
- **New types** in `types/api.ts`: `CommentDTO`, `CommentListResponse`, `CreateCommentRequest`, `CaptchaChallenge`.

### 8.2 Mounting
- `app/properties/[id]/page.tsx`: insert `<PropertyComments propertyId={id} />` after the description / price-history block.
- The component is a server-rendered shell that fetches the first page on the server (good for SEO and TTFB) and hydrates a client wrapper for the composer + interactions.

### 8.3 i18n keys
```
comments.heading           "Bình luận ({count})"
comments.empty             "Chưa có bình luận — hãy là người đầu tiên."
comments.composerNamePlaceholder "Tên của bạn"
comments.composerBodyPlaceholder "Viết bình luận của bạn…"
comments.submit            "Gửi bình luận"
comments.cancel            "Hủy"
comments.reply             "Trả lời"
comments.edit              "Sửa"
comments.delete            "Xóa"
comments.report            "Báo cáo"
comments.like              "Thích"
comments.captchaQuestion   "{a} {op} {b} = ?"
comments.captchaAnswerLabel "Nhập kết quả"
comments.guestBadge        "Khách"
comments.ownerBadge        "Chủ tin"
comments.adminBadge        "Quản trị"
comments.editedSuffix      "(đã sửa)"
comments.hidden            "Bình luận này đã bị ẩn"
comments.rateLimited       "Bạn bình luận quá nhiều — thử lại sau {seconds}s"
comments.errorBodyTooLong  "Bình luận quá dài (tối đa 1000 ký tự)"
comments.errorCaptcha      "Câu trả lời không đúng — thử lại"
```

### 8.4 Navigation / discoverability
- Property card shows a small `💬 N` count next to the like badge (uses the batch counts endpoint, see §7.2 future-only).
- Admin gets a new entry in the admin nav: `Bình luận đang chờ xử lý` linking to `/admin/comments`.

---

## 9. Non-functional requirements

| Concern | Target |
|---|---|
| Latency p50 (GET list) | ≤ 80 ms |
| Latency p95 (GET list) | ≤ 200 ms |
| Latency p95 (POST) | ≤ 400 ms incl. property existence check (cached) |
| Availability | 99.5%. Read-only fallback if DB primary down — cached SSR list still renders. |
| Per-IP write rate | 5 / 5 min, 30 / day (anon); doubled for logged-in |
| Per-property write rate | 20 / min |
| Per-IP like rate | 60 / min, 500 / day |
| Body length | 1–1000 chars (UTF-8) |
| Display name length | 1–50 chars |
| Edit window | 15 min after create |
| Soft-delete retention | 30 days; hard-purge job runs daily |
| Logged data | comment row, hashed IP, truncated UA, hashed email. **No raw PII.** |
| Backups | Daily pg_dump of `commentsdb`, 30-day retention |

---

## 10. Spam / abuse mitigation

Layered defense — none of these alone is enough; combined they make spam economically uninteresting.

| Layer | Mitigation | Cost to attacker | Cost to legit user |
|---|---|---|---|
| 1. Honeypot | Hidden `website` field; non-empty → silent 200 | high (must inspect form) | zero |
| 2. Captcha | Math captcha (`a + b`, etc.) for anon writes only | medium (OCR is solved but not free) | ~3 s |
| 3. Rate limit (IP) | 5 writes / 5 min, 30 / day | medium (need IP rotation) | invisible |
| 4. Rate limit (property) | 20 / min total | high (can't dogpile a single listing) | invisible |
| 5. Content length | 1–1000 chars | low | invisible |
| 6. Profanity filter | Vietnamese + English wordlist, **flag-only** (admin sees, doesn't auto-block) | low | zero (no false-block) |
| 7. URL allowlist | More than 2 URLs in body → captcha re-challenge | medium | small (legit users rarely paste 3+ URLs) |
| 8. Report → moderation | Anyone can report; admin queue | high (many reporters) | zero |
| 9. Soft-delete + audit | Hidden comments preserved 30 days; hard purge | n/a | n/a (admin recovery) |
| 10. Kill-switch | `COMMENTS_DISABLED=true` env var disables writes site-wide in seconds | n/a | global (only when triggered) |

We deliberately **do not** add: IP geo-blocking (false-positives for VPN users), CAPTCHA-on-every-write (kills UX), Akismet-like third party (cost + privacy concerns for VN data). Revisit if abuse exceeds 5% of traffic.

### 10.1 Privacy of moderation data
- Only ROLE_ADMIN sees IP hashes and full UAs.
- Hashes use HMAC with a server secret (rotated yearly) so the audit trail can't be reverse-correlated to specific people via rainbow tables.
- Reports include the reporter's identity (or hashed IP) so admins can spot retaliatory false reports.

---

## 11. Out-of-scope / v2 candidates

These are real but explicitly deferred so v1 ships in 2 weeks not 8.

1. **Email notifications** when someone replies to your comment. Needs SES/Mailgun integration and unsubscribe flow.
2. **Mentions (`@user`)** with autocomplete from auth-service.
3. **Markdown / image attachments** — bumps abuse surface significantly; need image moderation pipeline first.
4. **Real-time updates** via SSE or websockets so a viewer sees new comments without refresh.
5. **Comment search** across all properties.
6. **Threaded > 1 level** — only if user research shows demand.
7. **Reactions beyond 👍** (heart, fire, question) — each new reaction adds noise; keep simple in v1.
8. **`commentCount` denormalized on Property** so listing cards show "💬 14" — needs Kafka subscription pipeline (§7.2).
9. **AI moderation** — flag suspicious comments using Claude Haiku (paid). Worth the cost only if human moderation queue gets > 100 items/day.
10. **Karma / reputation** for logged-in commenters — incentivizes returning posters but adds gamification surface area.

---

## 12. Success metrics (review @ 4 weeks post-launch)

| Metric | Definition | Target |
|---|---|---|
| **Adoption** | % of property detail page sessions that scroll past the comments section | ≥ 40% |
| **Engagement** | % of property detail sessions that submit at least one comment | ≥ 5% (anon) + ≥ 8% (logged-in) |
| **Reply rate** | % of top-level comments that get a reply within 48 h | ≥ 25% |
| **Spam ratio** | hidden / total comments | ≤ 3% |
| **Moderation backlog** | reports awaiting review | ≤ 50 at any time |
| **POST p95 latency** | end-to-end including captcha | ≤ 500 ms |
| **GET p95 latency** | first-page list | ≤ 200 ms |
| **Service availability** | uptime including DB failover | ≥ 99.5% |

Mis-moderation review: weekly, sample 50 hidden comments → confirm hide was justified → tune profanity wordlist + admin training.

---

## 13. Rollout plan

| Week | Milestone |
|---|---|
| 1 | Skeleton comment-service + Flyway schema + GET endpoint returning empty list; Kong route; CommentList UI behind `NEXT_PUBLIC_COMMENTS=1` |
| 1 | POST endpoint with rate-limit + captcha + honeypot; integration tests covering anon + logged-in paths |
| 2 | Reply / edit / delete / like / report endpoints; admin moderation queue UI |
| 2 | Profanity filter, soft-delete + 30-day purge job, kill-switch env var |
| 3 | Internal dogfood: enable for ROLE_ADMIN only on 3 hand-picked properties |
| 3 | Public beta: enable site-wide; comment count badge on property cards (batch endpoint); collect metrics |
| 4 | Review metrics; tune rate limits; decide on v2 priorities |

Rollback plan: `COMMENTS_DISABLED=true` hides the section frontend-side and 503s POSTs server-side. Existing rows are preserved.

---

## 14. Open questions

1. **Captcha — math vs hCaptcha vs Cloudflare Turnstile?** Math is zero-dep but ~80% effective against bots; hCaptcha/Turnstile is ~99% but adds an external dep + privacy review. *Recommend math for v1; instrument the spam ratio (§12) and switch to Turnstile if > 3% in week 1.*
2. **Should logged-in users still see a captcha for their first comment ever?** Reduces spam from drive-by signups but adds friction. *Recommend no — skip captcha for any logged-in account ≥ 24 h old, captcha for fresh accounts.*
3. **How do we handle comments on a deleted property?** Cascade-delete the comment rows, or preserve them orphaned? *Recommend cascade — comments without a property are useless and orphan rows complicate moderation.*
4. **Editing window — 15 min, 1 h, or "until first reply"?** Longer windows let users fix typos but enable bait-and-switch (post a question, get a reply, edit the question to something else). *Recommend 15 min — strong enough to let users fix typos, short enough to make bait-and-switch impractical.*
5. **Should anonymous commenters' display names be unique-per-property?** Stops a single bad actor from impersonating "Hà (chủ tin)" repeatedly. *Recommend yes — 24 h soft uniqueness keyed on `(property_id, lower(name))`. Conflict yields a polite "name already used today, try another".*
6. **Where does the captcha secret / IP hash key live?** Same answer as the AI search Anthropic key: `.env.local` (gitignored) for dev, k8s Secret for prod.
7. **GDPR / VN PDPL — right-to-erasure on anon comments?** No `user_id` to attach the request to. *Recommend an out-of-band admin tool to find-and-hide by `ip_hash` if the requester can prove their IP.*

---

## Appendix A — Database schema (V1)

```sql
CREATE TABLE comments (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    parent_id       BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    user_id         VARCHAR(64),                       -- nullable for anon
    guest_name      VARCHAR(50),                       -- required when user_id is null
    gravatar_hash   CHAR(32),                          -- optional, MD5 of email
    body            TEXT NOT NULL CHECK (length(body) BETWEEN 1 AND 1000),
    ip_hash         CHAR(64),                          -- HMAC-SHA256 hex
    user_agent      VARCHAR(200),
    like_count      INTEGER NOT NULL DEFAULT 0,
    reply_count     INTEGER NOT NULL DEFAULT 0,
    flags           JSONB NOT NULL DEFAULT '{}'::jsonb, -- e.g. {"profanity": true}
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    hidden_at       TIMESTAMP,
    hidden_reason   VARCHAR(200),
    hidden_by       VARCHAR(64),                       -- admin user_id
    CHECK (user_id IS NOT NULL OR guest_name IS NOT NULL),
    -- depth ≤ 1 enforced in service layer; cheap trigger could backstop
);
CREATE INDEX idx_comments_property_created ON comments(property_id, created_at DESC) WHERE parent_id IS NULL;
CREATE INDEX idx_comments_parent ON comments(parent_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_ip_hash_created ON comments(ip_hash, created_at);
CREATE INDEX idx_comments_hidden ON comments(hidden_at) WHERE hidden_at IS NOT NULL;

CREATE TABLE comment_likes (
    comment_id      BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    identity        VARCHAR(64) NOT NULL,              -- user_id or ip_hash
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, identity)
);

CREATE TABLE comment_reports (
    id              BIGSERIAL PRIMARY KEY,
    comment_id      BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter        VARCHAR(64) NOT NULL,              -- user_id or ip_hash
    reason          VARCHAR(40) NOT NULL,              -- 'spam', 'abuse', 'off-topic', 'other'
    note            VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP,
    resolved_by     VARCHAR(64),                       -- admin user_id
    UNIQUE (comment_id, reporter)
);
```

## Appendix B — File touchpoints (estimated)

```
NEW  comment-service/                     (mirrors news-service skeleton, ~25 files)
NEW  frontend/components/PropertyComments.tsx
NEW  frontend/components/CommentComposer.tsx
NEW  frontend/components/CommentItem.tsx
NEW  frontend/components/CommentList.tsx
NEW  frontend/lib/api/commentApi.ts
EDIT frontend/types/api.ts                  (+ Comment* types)
EDIT frontend/app/properties/[id]/page.tsx  (mount PropertyComments)
EDIT frontend/messages/vi.json              (+ comments.* keys)
EDIT frontend/messages/en.json
EDIT kong/config/kong-local.yaml            (+ comment-service service & routes)
EDIT scripts/start-all-services.sh          (+ start comment-service step)
EDIT scripts/stop-all-services.sh
EDIT scripts/status-all-services.sh
NEW  frontend/app/admin/comments/page.tsx   (moderation queue)
EDIT components/Header.tsx                  (admin nav entry)
```

## Appendix C — Example state transitions

**Anonymous comment lifecycle (happy path)**:
```
1. GET /api/captcha               → { captchaId, question: "3 + 4 = ?" }
2. POST /api/properties/17/comments
     { body, displayName, captchaId, captchaAnswer: "7", website: "" }
   → 201 { id: 1042, ... }
3. (15 min later) PUT /api/comments/1042
   → 403 EDIT_WINDOW_EXPIRED
```

**Spam attempt (honeypot caught)**:
```
1. POST /api/properties/17/comments
     { body, displayName, captchaId, captchaAnswer, website: "https://buy-cheap.example" }
   → 200 { id: -1, status: "ok" }   ← lie; no row inserted
```

**Admin hide flow**:
```
1. POST /api/admin/comments/1042/hide
     { reason: "spam" }
   → 200; row updated, hidden_at = now, hidden_by = adminId
2. Future GET responses render the row as { hidden: true, body: null }
3. (30 days later) hard-purge job DELETEs the row
```
