# Real Estate Frontend

Next.js 14 frontend application for the Real Estate platform.

## 🚀 Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Authentication**: NextAuth.js
- **State Management**: React Hooks

## 📦 Installation

```bash
# Install dependencies
npm install
```

## 🏃 Development

```bash
# Run development server
npm run dev
```

Application will be available at `http://localhost:3000`

## 🏗️ Project Structure

```
frontend/
├── app/                    # Next.js App Router
│   ├── api/               # API routes (mock data)
│   ├── feed/              # Feed page
│   ├── login/             # Login page
│   ├── properties/        # Property pages
│   ├── search/            # Search page
│   └── layout.tsx         # Root layout
├── components/            # React components
│   ├── Header.tsx         # Navigation header
│   ├── PostCard.tsx       # Post display
│   ├── PostFeed.tsx       # Feed container
│   ├── PostForm.tsx       # Create post form
│   ├── LikeButton.tsx     # Like/unlike button
│   └── ...
├── lib/                   # Utilities
│   ├── auth.ts            # NextAuth configuration
│   └── mockData.ts        # Mock data (temporary)
├── types/                 # TypeScript types
│   └── index.ts           # Shared type definitions
├── public/                # Static assets
└── package.json           # Dependencies

```

## ✨ Features

### Implemented
- ✅ Property listing and search
- ✅ Property filtering (price, type, location)
- ✅ User authentication (login/register)
- ✅ Create property listings
- ✅ Social media feed with infinite scroll
- ✅ Like/unlike posts
- ✅ Responsive design

### Planned
- ⏳ Comments on posts
- ⏳ Media uploads
- ⏳ User profiles
- ⏳ Real-time notifications

## 🔧 Configuration

### Environment Variables

Create a `.env.local` file:

```env
NEXTAUTH_SECRET=your-secret-key
NEXTAUTH_URL=http://localhost:3000
```

### Tailwind CSS

Configuration in `tailwind.config.ts`. Custom colors and utilities defined.

## 🧪 Testing

```bash
# Run tests
npm test

# Run tests in watch mode
npm test -- --watch
```

## 📦 Building

```bash
# Build for production
npm run build

# Start production server
npm start
```

## 🔗 API Integration

Currently uses mock data from `lib/mockData.ts`.

**Migration to backend API (planned):**
1. Replace mock API routes with backend calls
2. Update `lib/` to use fetch/axios
3. Add error handling and loading states
4. Implement authentication with backend JWT

## 🎨 Styling

- **Tailwind CSS** for utility-first styling
- **Custom components** with consistent design system
- **Responsive breakpoints**: sm (640px), md (768px), lg (1024px), xl (1280px)
- **Primary color**: Blue (#2563eb)

## 📝 Code Style

- TypeScript strict mode enabled
- ESLint for code quality
- Components use functional style with hooks
- File naming: PascalCase for components, camelCase for utilities

## 🔐 Authentication

NextAuth.js with credentials provider:
- Login: `user@example.com` / `password123`
- Admin: `admin@example.com` / `admin123`

**Note:** Currently uses mock authentication. Will be replaced with backend JWT.

## 📱 Pages

- `/` - Home page with featured properties
- `/search` - Property search with filters
- `/properties/new` - Create new property listing (auth required)
- `/properties/[id]` - Property detail page
- `/feed` - Social media feed
- `/login` - Login page
- `/register` - Registration page

## 🚀 Deployment

Build and deploy:

```bash
npm run build
```

Deploy to Vercel, Netlify, or any Next.js-compatible platform.

## 📄 License

Private project
