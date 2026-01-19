# Real Estate Application - Monorepo

Full-stack real estate application with Next.js frontend and Spring Boot backend.

## 📁 Monorepo Structure

```
real-estate-ui/
├── frontend/              # Next.js frontend application
│   ├── app/              # Next.js app directory (routes, pages)
│   ├── components/       # React components
│   ├── lib/              # Utility functions and mock data
│   ├── types/            # TypeScript type definitions
│   ├── public/           # Static assets
│   ├── package.json      # Frontend dependencies
│   └── .gitignore        # Frontend-specific ignores
├── backend/              # Spring Boot backend API
│   ├── src/
│   │   ├── main/java/   # Java source code
│   │   └── resources/   # Configuration files
│   ├── pom.xml          # Maven dependencies
│   ├── mvnw             # Maven Wrapper
│   ├── .gitignore       # Backend-specific ignores
│   └── README.md        # Backend documentation
├── .gitignore           # Root-level ignores
└── README.md            # This file (monorepo overview)
```

## 🚀 Tech Stack

### Frontend
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Authentication**: NextAuth.js
- **State Management**: React Hooks

### Backend
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: H2 (development), PostgreSQL (production)
- **ORM**: Spring Data JPA
- **Utilities**: Lombok

## 🏃 Getting Started

### Prerequisites

- **Node.js**: 18+ (for frontend)
- **Java**: 17+ (for backend)
- **Maven**: Not required - Backend uses Maven Wrapper (mvnw)

### Run Frontend (Next.js)

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

Frontend will be available at: `http://localhost:3000`

### Run Backend (Spring Boot)

```bash
# Navigate to backend directory
cd backend

# Build and run with Maven Wrapper
./mvnw spring-boot:run

# Or use the startup script
./run.sh
```

Backend API will be available at: `http://localhost:8080`

## 📋 Available Features

### Frontend Features
- ✅ Property listing and search
- ✅ Property filtering (price, type, location)
- ✅ User authentication (login/register)
- ✅ Create property listings
- ✅ Social media feed with infinite scroll
- ✅ Like/unlike posts
- ✅ Responsive design

### Backend Features
- ✅ REST API structure
- ✅ Health check endpoint
- ⏳ User management (planned)
- ⏳ Property CRUD operations (planned)
- ⏳ Post management (planned)
- ⏳ Authentication & Authorization (planned)

## 🔗 API Endpoints

### Backend (Port 8080)
- `GET /api/health` - Health check

### Frontend API Routes (Port 3000)
- `POST /api/auth/[...nextauth]` - Authentication
- `GET/POST /api/posts` - Posts feed
- `POST/DELETE /api/posts/[postId]/like` - Like/unlike posts
- `POST /api/properties` - Create property

## 🗂️ Environment Variables

### Frontend (frontend/.env.local)
```env
NEXTAUTH_SECRET=your-secret-key
NEXTAUTH_URL=http://localhost:3000
```

### Backend (backend/src/main/resources/application.properties)
Already configured in `backend/src/main/resources/application.properties`

## 📝 Development Workflow

1. **Frontend Development**: Make changes in `frontend/app/`, `frontend/components/`, or `frontend/lib/`
2. **Backend Development**: Make changes in `backend/src/main/java/`
3. **Type Definitions**: Update types in `frontend/types/index.ts`
4. **API Integration**: Connect frontend to backend APIs (next phase)

## 🧪 Testing

### Frontend
```bash
cd frontend
npm test
```

### Backend
```bash
cd backend
./mvnw test
```

## 📦 Building for Production

### Frontend
```bash
cd frontend
npm run build
npm start
```

### Backend
```bash
cd backend
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## 🔄 Migration Plan

Currently, the frontend uses mock data. The migration plan:

1. ✅ Set up Spring Boot backend
2. ⏳ Create entity models (User, Property, Post, Like)
3. ⏳ Implement repositories and services
4. ⏳ Create REST controllers matching existing API routes
5. ⏳ Update frontend to use backend APIs
6. ⏳ Add authentication integration
7. ⏳ Deploy to production

## 👥 Contributors

- Frontend: Next.js + TypeScript
- Backend: Spring Boot + Java

## 📄 License

Private project
