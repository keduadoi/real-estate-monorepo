# Real Estate App Implementation Plan

## Overview
Build a Next.js real estate app with property listings, search, authentication, and property posting capabilities using mock data.

**Tech Stack:** Next.js 14 (App Router), TypeScript, Tailwind CSS, NextAuth.js

## Project Structure

```
real-estate-ui/
├── app/
│   ├── layout.tsx                    # Root layout with Header & providers
│   ├── page.tsx                      # Home - property listings (paginated)
│   ├── globals.css                   # Tailwind base styles
│   ├── api/auth/[...nextauth]/route.ts  # NextAuth API handler
│   ├── properties/
│   │   ├── [id]/page.tsx            # Property detail with 5 images
│   │   └── new/page.tsx             # Create property (auth required)
│   ├── login/page.tsx               # Login page
│   └── search/page.tsx              # Search results (no auth)
├── components/
│   ├── Header.tsx                   # Nav with login/post buttons
│   ├── PropertyCard.tsx             # Property preview card
│   ├── PropertyGrid.tsx             # Grid layout for cards
│   ├── SearchBar.tsx                # Search input with filters
│   ├── Pagination.tsx               # Page navigation
│   ├── ImageGallery.tsx             # 5-image viewer for details
│   └── PropertyForm.tsx             # Create property form
├── lib/
│   ├── auth.ts                      # NextAuth configuration
│   ├── mockData.ts                  # 100 mock properties generator
│   └── utils.ts                     # Pagination & filter helpers
├── types/
│   └── index.ts                     # TypeScript interfaces
├── public/images/properties/        # Property images (500 files)
├── package.json
├── tsconfig.json
├── tailwind.config.ts
├── next.config.js
└── .env.local                       # NEXTAUTH_SECRET, NEXTAUTH_URL
```

## Data Structure

**Property Interface:**
```typescript
{
  id: string;                        // "prop-1" to "prop-100"
  title: string;                     // e.g., "Villa 1 tầng tại Hà Nội"
  description: string;
  price: number;                     // VND
  address: string;
  city: string;                      // Hà Nội, HCM, Đà Nẵng, etc.
  bedrooms: number;
  bathrooms: number;
  area: number;                      // square meters
  propertyType: 'house' | 'apartment' | 'villa' | 'townhouse';
  status: 'for-sale' | 'for-rent';
  images: string[];                  // Array of 5 image paths
  createdAt: string;
  userId: string;
  features: string[];                // ['parking', 'garden', etc.]
}
```

**Mock Data:**
- Generate 100 properties with Vietnamese addresses
- Each property has 5 images (all using `/Users/ducnt/Downloads/thiet-ke-nha-ong-1-tang.jpg`)
- 2 mock users for authentication (email/password)

## Implementation Steps

### Phase 1: Project Initialization
1. Run `npx create-next-app@latest` with TypeScript, Tailwind, App Router
2. Install dependencies: `npm install next-auth`
3. Create `.env.local` with:
   ```
   NEXTAUTH_URL=http://localhost:3000
   NEXTAUTH_SECRET=<generated-secret>
   ```

### Phase 2: Type Definitions & Data
4. Create `types/index.ts` - Define Property, User, SearchFilters, PaginationParams interfaces
5. Create `lib/mockData.ts`:
   - `generateMockProperties(100)` function
   - Mock 100 properties with Vietnamese data
   - 2 mock users: `user@example.com` / `password123`, `admin@example.com` / `admin123`
6. Create `lib/utils.ts`:
   - `paginateArray()` - Slice array by page
   - `filterProperties()` - Filter by search criteria
   - `formatPrice()` - Format VND currency

### Phase 3: Image Setup
7. Create `public/images/properties/` directory
8. Copy mock image 500 times:
   - Name pattern: `property-{1-100}-{1-5}.jpg`
   - All using same source: `/Users/ducnt/Downloads/thiet-ke-nha-ong-1-tang.jpg`
   - Script or manual copy

### Phase 4: Authentication
9. Create `lib/auth.ts`:
   - NextAuth config with CredentialsProvider
   - Validate against mock users
   - JWT and session callbacks
10. Create `app/api/auth/[...nextauth]/route.ts` - Export NextAuth handler
11. Create `app/login/page.tsx`:
    - Login form (email/password)
    - Call `signIn()` from next-auth
    - Redirect after success

### Phase 5: Core Components
12. Create `components/Header.tsx`:
    - Logo and navigation
    - Login/Logout button (show based on session)
    - "Post Property" button (only when logged in)
    - Responsive mobile menu
13. Create `components/PropertyCard.tsx`:
    - Display first image, title, price, location
    - Show bedrooms, bathrooms, area
    - Link to `/properties/[id]`
    - Tailwind card styling
14. Create `components/PropertyGrid.tsx`:
    - 3-column grid (responsive)
    - Map properties to PropertyCard
    - Empty state
15. Create `components/Pagination.tsx`:
    - Previous/Next buttons
    - Page number links
    - Use URL search params
16. Create `components/SearchBar.tsx`:
    - Search input
    - Filters: city, property type, price range, bedrooms, status
    - Submit to `/search?query=...`

### Phase 6: Main Pages
17. Create `app/layout.tsx`:
    - SessionProvider wrapper
    - Include Header component
    - Global CSS and fonts
18. Create `app/globals.css`:
    - Tailwind directives (@tailwind base, components, utilities)
    - Custom styles if needed
19. Create `app/page.tsx` (Home):
    - Server Component
    - Get page from searchParams (default 1)
    - Paginate mock properties (12 per page)
    - Render SearchBar, PropertyGrid, Pagination
20. Create `app/search/page.tsx`:
    - Server Component
    - Parse searchParams (query, filters, page)
    - Filter properties using `filterProperties()`
    - Paginate results
    - Show results count
    - Render PropertyGrid with filtered data

### Phase 7: Property Features
21. Create `components/ImageGallery.tsx`:
    - Display main image (large)
    - Thumbnail strip (5 images)
    - Click thumbnail to change main
    - Next/Previous arrows
    - Use Next.js Image component
22. Create `app/properties/[id]/page.tsx`:
    - Server Component with dynamic [id] param
    - Find property by ID from mock data
    - Render ImageGallery with 5 images
    - Display all property details
    - Format price with `formatPrice()`
    - Show features, contact section
23. Create `components/PropertyForm.tsx`:
    - Form fields: title, description, price, address, city, bedrooms, bathrooms, area, type, status, features
    - Image upload placeholder (note: mock 5 images for now)
    - Form validation (HTML5 + client-side checks)
    - Submit handler (will add to mock data)
24. Create `app/properties/new/page.tsx`:
    - Server Component
    - Check auth with `getServerSession()`
    - Redirect to `/login` if not authenticated
    - Render PropertyForm
    - Handle submission (add to mock array)
    - Success message and redirect to home

### Phase 8: Testing & Polish
25. Test all features:
    - Browse properties with pagination
    - Search and filter without login
    - Login with mock credentials
    - Post new property when logged in
    - View property details with 5 images
    - Logout functionality
26. Responsive design verification (mobile, tablet, desktop)
27. Add loading states (loading.tsx files)
28. Error handling (error.tsx files)
29. SEO metadata (generateMetadata in pages)

## Key Features Implementation

### 1. Pagination
- **Strategy:** URL search params (`?page=2`)
- **Per Page:** 12 properties
- **Total Pages:** Math.ceil(100 / 12) = 9 pages
- **Preserve Filters:** Include search params in pagination links

### 2. Search (No Login Required)
- **Location:** `/search` route accessible to all
- **Filters:**
  - Text query (searches title, description, address)
  - City dropdown
  - Property type (house/apartment/villa/townhouse)
  - Price range (min/max)
  - Bedrooms count
  - Status (for-sale/for-rent)
- **Implementation:** Server-side filtering in Search page component

### 3. Authentication (Login to Post)
- **Provider:** NextAuth with Credentials
- **Mock Users:** 2 test accounts in `lib/mockData.ts`
- **Protected Route:** `/properties/new` (redirects to login if unauthenticated)
- **UI Changes:** Header shows "Post Property" button only when logged in

### 4. Property Detail with 5 Images
- **Route:** `/properties/[id]`
- **Image Gallery:**
  - Main image viewer (large)
  - 5 thumbnail buttons below
  - Click to switch main image
  - Responsive sizing
- **Image Sources:** All point to copies of `/Users/ducnt/Downloads/thiet-ke-nha-ong-1-tang.jpg`

## Critical Files

**Order of Implementation:**
1. `types/index.ts` - Base type definitions
2. `lib/mockData.ts` - Data source for entire app
3. `lib/utils.ts` - Helper functions
4. `lib/auth.ts` - Auth configuration
5. `app/api/auth/[...nextauth]/route.ts` - Auth endpoints
6. `components/PropertyCard.tsx` - Core reusable component
7. `app/layout.tsx` - Root layout
8. `app/page.tsx` - Main landing page

## Verification Steps

After implementation, verify:
1. **Property Listing:** Visit `/` - see 12 properties with pagination
2. **Pagination:** Click page 2 - URL changes to `/?page=2`, shows next 12 properties
3. **Search:** Use search bar - redirects to `/search?query=villa`, filters work
4. **Property Detail:** Click property card - opens `/properties/prop-1`, shows 5 images
5. **Image Gallery:** Click thumbnails - main image changes
6. **Login Required:** Try accessing `/properties/new` - redirects to `/login`
7. **Login:** Login with `user@example.com` / `password123` - succeeds
8. **Post Property:** After login, "Post Property" button appears in header
9. **Create Property:** Fill form at `/properties/new`, submit - success
10. **Logout:** Click logout - session cleared, "Post Property" button disappears

## Dependencies

```json
{
  "dependencies": {
    "next": "^14.1.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "next-auth": "^4.24.5"
  },
  "devDependencies": {
    "@types/node": "^20.11.0",
    "@types/react": "^18.2.48",
    "typescript": "^5.3.3",
    "tailwindcss": "^3.4.1",
    "autoprefixer": "^10.4.17",
    "postcss": "^8.4.33"
  }
}
```

## Notes

- All data is **in-memory mock data** - refreshing the server will reset any added properties
- Future: Replace mock data with real database (PostgreSQL, MongoDB)
- Future: Implement real image upload (Cloudinary, AWS S3)
- Future: Add user registration, password hashing, email verification
- Vietnamese language support in mock data for realistic content
- Responsive design using Tailwind breakpoints (sm, md, lg, xl)
