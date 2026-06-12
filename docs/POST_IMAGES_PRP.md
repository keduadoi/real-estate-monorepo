# Product Requirements Proposal (PRP)
# Post Images - Image Attachments for the Social Feed

**Version**: 1.0
**Date**: 2026-06-12
**Author**: Engineering Team
**Status**: Draft - Pending Review
**Depends on**: [POST_SERVICE_PRP.md](./POST_SERVICE_PRP.md) (implemented)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Objectives](#3-goals-and-objectives)
4. [Architecture Decision: Image Upload Flow](#4-architecture-decision-image-upload-flow)
5. [Database Schema & Migration](#5-database-schema--migration)
6. [Backend Changes (post-service)](#6-backend-changes-post-service)
7. [API Changes](#7-api-changes)
8. [Frontend Changes](#8-frontend-changes)
9. [Validation & Security](#9-validation--security)
10. [Testing Strategy](#10-testing-strategy)
11. [Implementation Plan](#11-implementation-plan)
12. [Success Metrics](#12-success-metrics)
13. [Future Considerations](#13-future-considerations)

---

## 1. Executive Summary

### 1.1 Overview

The social feed (post-service + `/feed` page) currently supports **text-only posts with likes**. This PRP adds **image attachments** so users can create posts with up to 10 photos, displayed in a responsive grid with a fullscreen lightbox — matching the experience of a typical social network.

This was explicitly listed as out-of-scope in POST_SERVICE_PRP.md §3.2 ("Image/media attachments in posts") and is now being brought into scope.

### 1.2 Current State

```
PostForm (textarea only) ──▶ POST /api/posts { content }
                                    │
                                    ▼
                          posts table (content TEXT)
                                    │
                                    ▼
PostCard renders text + LikeButton — no media support
```

- `Post` entity / `posts` table: `content`, `user_id`, denormalized author fields, timestamps
- `CreatePostRequest` / `UpdatePostRequest`: `content` only (1–5000 chars, required)
- Image upload infrastructure **already exists** for properties:
  - property-service `FileUploadController` → `POST /api/upload/temp` (via Kong)
  - `StorageService` abstraction with `LocalStorageService` / `S3StorageService`
  - Frontend `useImageUpload` hook + `ImageUpload` component + `/api/upload/temp` Next.js proxy route (with local fallback)

### 1.3 Target State

```
PostForm (textarea + image picker, max 10)
    │
    │ 1. POST /api/upload/temp (multipart) ──▶ returns imageUrls[]
    │ 2. POST /api/posts { content, imageUrls }
    ▼
post-service
    │
    ├── posts table (content now optional if images present)
    └── post_images table (post_id, image_url, sort_order)
    ▼
PostCard renders text + image grid (1/2/3/4+ layouts)
    └── click image ──▶ fullscreen lightbox (shared with ImageGallery)
```

### 1.4 Key Decisions (summary)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Where images are uploaded | Reuse existing `/api/upload/temp` (property-service storage) | Zero new storage infrastructure; flow already proven for properties |
| How images are stored in post-service | Separate `post_images` table with `sort_order` | Preserves ordering, cascades on delete, mirrors `likes` pattern |
| Image-only posts | Allowed | Standard social-network behavior; validation becomes "content OR ≥1 image" |
| Lightbox | Extract shared `ImageLightbox` from `ImageGallery` | The gallery's fullscreen mode was just built; reuse instead of duplicating |

---

## 2. Problem Statement

### 2.1 Current Limitations

1. **Text-only feed**: Users cannot share photos of properties, neighborhoods, or market materials — the primary content type for a real-estate community.
2. **Inconsistent UX**: Property listings support rich image galleries, but the social feed does not.
3. **Lower engagement**: Image posts drive significantly more engagement than text on comparable platforms.

### 2.2 User Stories

| ID | Story | Priority |
|----|-------|----------|
| US-1 | As a user, I want to attach one or more images when creating a post | High |
| US-2 | As a user, I want to preview and remove selected images before posting | High |
| US-3 | As a user, I want to post images without any text | Medium |
| US-4 | As a user, I want to see post images in a clean grid in my feed | High |
| US-5 | As a user, I want to click a post image to view it fullscreen and navigate between images | High |
| US-6 | As a user, I want images to keep the order I attached them in | Medium |
| US-7 | As a post owner, when I delete my post its images are no longer referenced | High |

---

## 3. Goals and Objectives

### 3.1 Primary Goals

1. Support **0–10 images per post** end to end (create, read, display)
2. **Reuse existing upload infrastructure** (no new storage service)
3. Responsive **image grid** in `PostCard` matching common social-network layouts
4. **Fullscreen lightbox** with keyboard navigation, shared with the property `ImageGallery`
5. Full **i18n** (en/vi) for all new UI strings

### 3.2 Non-Goals (Out of Scope for v1)

- Video or other media types
- Image editing (crop, rotate, filters)
- Editing the image set on an existing post (`updatePost` keeps text-only edits in v1; images are set at creation)
- Alt-text input for accessibility (auto-generated alt used instead)
- Garbage collection of orphaned uploads (tracked in §13)
- CDN / image resizing pipeline

### 3.3 Success Criteria

- [ ] User can create a post with up to 10 images from `/feed`
- [ ] Image-only post (empty text) is accepted
- [ ] Feed renders grids correctly for 1, 2, 3, 4, and 5+ images
- [ ] Clicking any image opens the lightbox at that image; arrows/Escape work
- [ ] Existing text-only posts continue to render unchanged
- [ ] All post-service tests pass; new scenarios covered

---

## 4. Architecture Decision: Image Upload Flow

### 4.1 Options Considered

| Option | Description | Verdict |
|--------|-------------|---------|
| **A. Reuse `/api/upload/temp`** (property-service) | Frontend uploads files first, then sends resulting URLs in `CreatePostRequest` | ✅ **Chosen** |
| B. New upload endpoint in post-service | Copy `StorageService` into post-service, `POST /api/posts/upload` | Duplicates storage config/credentials across services for no isolation benefit in v1 |
| C. Multipart `POST /api/posts` | Single request with files + content | Couples post creation to file handling; complicates Kong config and retries |

### 4.2 Chosen Flow (Option A)

```
User        PostForm        Next.js /api/upload/temp     Kong      property-service     post-service
 │              │                     │                    │              │                  │
 │─select imgs─▶│ (preview locally)   │                    │              │                  │
 │─click Post──▶│                     │                    │              │                  │
 │              │──multipart files───▶│──forward──────────▶│─────────────▶│ store files      │
 │              │◀──{ imageUrls[] }───│◀───────────────────│◀─────────────│                  │
 │              │──POST /api/posts { content, imageUrls }─▶│──────────────────────────────-─▶│ persist
 │              │◀──201 PostResponse (with imageUrls)──────│◀─────────────────────────────---│
 │◀─new post────│                     │                    │              │                  │
```

Notes:
- post-service stores **URLs only** — it never touches file bytes. This keeps the service stateless with respect to storage.
- The existing Next.js proxy route (`frontend/app/api/upload/temp/route.ts`) already handles auth headers and a local-disk fallback when the backend is unavailable; no changes needed.
- URLs returned by the backend are normalized on display via the existing `fixImageUrl()` util (`frontend/lib/utils.ts`).

---

## 5. Database Schema & Migration

### 5.1 Migration: `V3__create_post_images_table.sql`

```sql
-- Image attachments for posts
CREATE TABLE post_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    image_url       VARCHAR(1024) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_post_images_post_id ON post_images(post_id);

COMMENT ON TABLE post_images IS 'Image attachments for social feed posts';
COMMENT ON COLUMN post_images.sort_order IS 'Display order of images within a post (0-based)';
```

Additionally, relax the content constraint to allow image-only posts:

```sql
-- V4__allow_empty_post_content.sql
ALTER TABLE posts ALTER COLUMN content DROP NOT NULL;
```

(The "content OR images required" rule is enforced at the service layer — see §6.3 — because the DB cannot express the cross-table constraint cheaply.)

### 5.2 Schema Diagram (delta)

```
┌──────────────────────────┐       ┌──────────────────────────────┐
│          posts           │ 1   N │         post_images           │
│  content TEXT (nullable) │──────▶│  post_id    UUID FK (CASCADE) │
└──────────────────────────┘       │  image_url  VARCHAR(1024)     │
                                   │  sort_order INT               │
                                   └──────────────────────────────┘
```

---

## 6. Backend Changes (post-service)

### 6.1 New Entity: `PostImage`

```java
@Entity
@Table(name = "post_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @NotBlank
    @Size(max = 1024)
    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

### 6.2 `Post` Entity Changes

```java
// content: remove @NotBlank (cross-field rule moves to service layer);
// keep @Size(max = 5000)
@Size(max = 5000, message = "Content must be at most 5000 characters")
@Column(columnDefinition = "TEXT")
private String content;

@OneToMany(mappedBy = "post", cascade = CascadeType.ALL,
           orphanRemoval = true, fetch = FetchType.LAZY)
@OrderBy("sortOrder ASC")
@Builder.Default
private List<PostImage> images = new ArrayList<>();
```

### 6.3 `PostService` Changes

```java
private static final int MAX_IMAGES_PER_POST = 10;

public PostResponse createPost(CreatePostRequest request) {
    // ... existing auth resolution ...

    boolean hasContent = request.content() != null && !request.content().isBlank();
    boolean hasImages = request.imageUrls() != null && !request.imageUrls().isEmpty();
    if (!hasContent && !hasImages) {
        throw new InvalidPostException("Post must have content or at least one image");
    }

    Post post = new Post();
    post.setContent(hasContent ? request.content().trim() : null);
    // ... existing user fields ...

    if (hasImages) {
        List<String> urls = request.imageUrls();
        for (int i = 0; i < urls.size(); i++) {
            post.getImages().add(PostImage.builder()
                .post(post)
                .imageUrl(urls.get(i))
                .sortOrder(i)
                .build());
        }
    }

    Post saved = postRepository.save(post);  // cascades images
    return mapToResponse(saved, userId);
}

// mapToResponse: add
//   .imageUrls(post.getImages().stream().map(PostImage::getImageUrl).toList())
```

`updatePost` keeps its current behavior (text edit only). Deleting a post cascades to `post_images` via FK + JPA cascade — no service change needed.

To avoid N+1 queries on the feed, fetch images alongside posts:

```java
// PostRepository
@EntityGraph(attributePaths = {"images"})
Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
```

### 6.4 New Exception

`InvalidPostException` → handled in `GlobalExceptionHandler` as `400 VALIDATION_ERROR`, consistent with existing handlers.

---

## 7. API Changes

**No new endpoints.** Existing endpoints gain an optional field.

### 7.1 `CreatePostRequest` (and DTO validation)

```java
public record CreatePostRequest(
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    String content,                       // now optional

    @Size(max = 10, message = "A post can have at most 10 images")
    List<@NotBlank @Size(max = 1024) String> imageUrls   // optional
) {}
```

(`@NotBlank` removed from `content`; the "content OR images" rule lives in `PostService` — see §6.3.)

### 7.2 `PostResponse`

```java
@Builder
public record PostResponse(
    UUID id,
    String content,            // may be null for image-only posts
    List<String> imageUrls,    // NEW — ordered, empty list for text-only posts
    PostAuthorResponse author,
    long likeCount,
    boolean isLikedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### 7.3 Example

```http
POST /api/posts
{
  "content": "Open house this Saturday!",
  "imageUrls": [
    "http://localhost:8080/uploads/temp/abc.jpg",
    "http://localhost:8080/uploads/temp/def.jpg"
  ]
}

201 Created
{
  "id": "…",
  "content": "Open house this Saturday!",
  "imageUrls": ["…abc.jpg", "…def.jpg"],
  "author": { … },
  "likeCount": 0,
  "isLikedByCurrentUser": false,
  …
}
```

Backward compatibility: clients that omit `imageUrls` behave exactly as today; old clients reading responses simply ignore the new field.

---

## 8. Frontend Changes

### 8.1 Types & API Client

`frontend/types/index.ts` and `frontend/lib/api/postApi.ts`:

```typescript
export interface Post {
  id: string;
  content: string | null;     // nullable for image-only posts
  imageUrls: string[];        // NEW
  author: PostAuthor;
  likeCount: number;
  isLikedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  content?: string;
  imageUrls?: string[];       // NEW
}
```

### 8.2 `PostForm` — image picker

Reuse the existing `useImageUpload` hook (already handles file selection, client-side validation, previews via object URLs, and upload to `/api/upload/temp`). UI additions to `PostForm.tsx`:

- A photo button (icon) below the textarea that opens the file picker (`accept="image/*"`, multiple)
- Thumbnail strip of selected images with per-image remove buttons and a counter (`{n}/10`)
- Submit flow: if images selected → upload via the hook → collect `imageUrls` → include in `postApi.create()`
- `isValid` becomes: `(content.trim().length > 0 || images.length > 0) && !isOverLimit`
- Disable form while uploading; surface upload errors in the existing error box

Mockup:

```
┌──────────────────────────────────────────────┐
│ What are you thinking about the real estate… │
│                                              │
├──────────────────────────────────────────────┤
│ [img] [img] [img] (×)        3/10            │
│ 📷 Add photos                    0/5000      │
│ [ Post ]  [ Cancel ]                         │
└──────────────────────────────────────────────┘
```

### 8.3 `PostCard` — image grid

New presentation block between content and the like bar, only rendered when `post.imageUrls?.length > 0`. Layouts (Facebook-style):

| Count | Layout |
|-------|--------|
| 1 | Single image, full width, max-height capped (e.g. `max-h-[500px]`, `object-cover`) |
| 2 | Two columns, equal halves |
| 3 | One large left, two stacked right |
| 4 | 2×2 grid |
| 5+ | 2×2 grid; 4th cell shows `+N` dark overlay |

All images use `next/image` with `fixImageUrl()` applied. Clicking any image (including the `+N` cell) opens the lightbox at that index.

### 8.4 Shared `ImageLightbox` component

Extract the fullscreen overlay just added to `ImageGallery.tsx` (commit `d4d3705`) into `frontend/components/ImageLightbox.tsx`:

```typescript
interface ImageLightboxProps {
  images: string[];
  initialIndex: number;
  title: string;       // used for alt text
  onClose: () => void;
}
```

Behavior carried over unchanged: backdrop + prev/next arrows + counter + close button, `←`/`→`/`Escape` keys, body-scroll lock. `ImageGallery` is refactored to use it (no behavior change); `PostCard` uses it for feed images.

### 8.5 i18n (`messages/en.json` / `vi.json`)

New keys under `components.postForm`:

| Key | en | vi |
|-----|----|----|
| `addPhotos` | "Add photos" | "Thêm ảnh" |
| `imageCounter` | "{current}/{max} images" | "{current}/{max} ảnh" |
| `removeImage` | "Remove image" | "Xóa ảnh" |
| `uploadFailed` | "Failed to upload images" | "Tải ảnh lên thất bại" |
| `emptyPost` | "Write something or add a photo" | "Hãy viết gì đó hoặc thêm ảnh" |

New keys under `components.postCard`:

| Key | en | vi |
|-----|----|----|
| `imageAlt` | "Photo {index} by {author}" | "Ảnh {index} của {author}" |
| `moreImages` | "+{count}" | "+{count}" |

Lightbox labels reuse the existing `components.imageGallery` keys (`previous`, `next`, `openFullscreen`, `closeFullscreen`).

---

## 9. Validation & Security

| Layer | Rule |
|-------|------|
| Frontend (`useImageUpload`) | Image MIME types only, per-file size limit (existing hook config), max 10 files |
| Upload endpoint (property-service) | Existing `StorageService` validation (type/size) applies unchanged; requires authentication |
| post-service DTO | `imageUrls` ≤ 10, each non-blank and ≤ 1024 chars |
| post-service service layer | "content OR ≥1 image" rule; content trimmed; create requires auth (existing `UserContext`) |
| Kong | No route changes — `/api/posts` and `/api/upload/temp` routes already exist |

**URL trust note**: post-service stores client-supplied URLs verbatim, same trust model as property-service's `images` field today. v1 accepts this (the feed only renders them as `<img>` sources through `next/image`, and `next.config.js` `remotePatterns` restricts allowed hosts). A stricter allowlist (only accept URLs from our own storage host) is listed as a fast-follow in §13.

---

## 10. Testing Strategy

### 10.1 Backend (post-service)

| Category | Test Case |
|----------|-----------|
| **Create with images** | Success with content + 3 images, order preserved |
| | Success with images only (no content) |
| | Success with content only (regression) |
| | Fail with no content and no images → 400 |
| | Fail with 11 images → 400 |
| | Fail with blank image URL → 400 |
| **Feed** | `imageUrls` populated in order on `GET /api/posts` |
| | Text-only posts return empty `imageUrls` list |
| | No N+1 image queries for a 20-post page (verify `@EntityGraph`) |
| **Delete** | Deleting a post removes its `post_images` rows (cascade) |
| **Migration** | V3/V4 apply cleanly on a database with existing posts |

### 10.2 Frontend

- `PostForm`: submit disabled when empty; enabled with images only; thumbnails render and remove correctly; counter caps at 10
- `PostCard`: grid snapshot tests for 1/2/3/4/5 images; `+N` overlay; text-only post unchanged
- `ImageLightbox`: opens at clicked index, keyboard nav, Escape closes; `ImageGallery` regression after refactor
- E2E happy path: create image post on `/feed` → appears in feed → lightbox opens

---

## 11. Implementation Plan

### Phase 1: Backend (post-service)

| Task | Description |
|------|-------------|
| 1.1 | Migrations V3 (`post_images`) and V4 (nullable content) |
| 1.2 | `PostImage` entity + `Post.images` relation (`@OrderBy`, cascade) |
| 1.3 | DTO updates (`CreatePostRequest.imageUrls`, `PostResponse.imageUrls`) |
| 1.4 | `PostService` create/map changes + `InvalidPostException` + handler |
| 1.5 | `@EntityGraph` on feed queries |
| 1.6 | Unit + repository tests (§10.1) |

### Phase 2: Frontend

| Task | Description |
|------|-------------|
| 2.1 | Update `Post` / `CreatePostRequest` types and `postApi` |
| 2.2 | Extract `ImageLightbox` from `ImageGallery`; refactor gallery to use it |
| 2.3 | `PostForm` image picker (reuse `useImageUpload`) + upload-then-create flow |
| 2.4 | `PostCard` image grid + lightbox integration |
| 2.5 | i18n keys (en/vi) |
| 2.6 | Component + E2E tests (§10.2) |

### Phase 3: Polish & Rollout

| Task | Description |
|------|-------------|
| 3.1 | Verify `next.config.js` image `remotePatterns` cover storage hosts |
| 3.2 | Manual cross-browser / mobile grid check |
| 3.3 | Update POST_SERVICE_PRP.md §3.2 to mark image attachments as delivered |

Phases 1 and 2.1–2.2 can proceed in parallel.

---

## 12. Success Metrics

| Metric | Target |
|--------|--------|
| Feed response time (p95) with images | < 250ms for 20 posts |
| Image post creation (excl. upload time) | < 300ms |
| Upload of 5 images (3MB each, local env) | < 5s |
| Test coverage of new backend code | > 80% |
| Regression: existing text-post tests | 100% pass |

---

## 13. Future Considerations

1. **Orphaned upload cleanup**: images uploaded but never attached to a post (user abandons the form) remain in temp storage. Add a scheduled cleanup of `uploads/temp` older than 24h, or move attached images out of temp on post creation (as property-service does for listings).
2. **URL allowlist**: validate that submitted `imageUrls` point at our own storage host before persisting.
3. **Image editing on update**: allow `updatePost` to add/remove/reorder images.
4. **Thumbnails / resizing pipeline**: serve feed-sized variants instead of originals.
5. **Video attachments**: the `post_images` table can generalize to `post_media` with a `media_type` column.

---

**Document Status**: Ready for Review
**Next Steps**: Approval → Implementation Phase 1
