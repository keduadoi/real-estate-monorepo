# Deployment Guide

## 🚀 Deploying to Vercel (Frontend)

### Prerequisites
- Vercel account connected to your GitHub repository
- Project imported from GitHub

### Configuration Steps

#### 1. Update Root Directory Setting

In your Vercel project dashboard:

1. Go to **Settings** → **General**
2. Find **Root Directory** section
3. Click **Edit** or **Override**
4. Set value to: `frontend`
5. Click **Save**

#### 2. Verify Build Configuration

Under **Build & Development Settings**, verify:

```
Framework Preset:     Next.js (auto-detected)
Build Command:        npm run build
Output Directory:     .next
Install Command:      npm install
Node.js Version:      18.x or 20.x
```

#### 3. Environment Variables

Add these in **Settings** → **Environment Variables**:

| Variable | Value | Environment |
|----------|-------|-------------|
| `NEXTAUTH_SECRET` | Your secret key | Production, Preview |
| `NEXTAUTH_URL` | `https://your-domain.vercel.app` | Production |
| `NEXTAUTH_URL` | `https://$VERCEL_URL` | Preview |

#### 4. Deploy

**Automatic Deployment:**
- Push to `main` or `develop` branch
- Vercel will auto-deploy

**Manual Deployment:**
- Go to **Deployments** tab
- Click **Redeploy** on any deployment

### Monorepo Structure

```
real-estate-ui/
├── frontend/          ← Vercel deploys this directory
│   ├── app/
│   ├── components/
│   ├── package.json
│   └── ...
├── backend/           ← Ignored by Vercel (.vercelignore)
└── .vercelignore      ← Excludes backend from deployment
```

### Troubleshooting

**Build fails?**
- Check that Root Directory is set to `frontend`
- Verify Node.js version (18.x or higher)
- Check environment variables are set

**API routes not working?**
- Frontend API routes (Next.js) will work: `/api/posts`, `/api/auth`
- These are mock data routes (temporary)
- Backend APIs won't be accessible from Vercel deployment yet

**404 on routes?**
- Next.js App Router handles all routes automatically
- No additional configuration needed

### Future: Backend Deployment

When ready to deploy the Spring Boot backend:

**Option 1: Railway**
- Deploy backend separately on Railway
- Update frontend API calls to point to Railway URL

**Option 2: Heroku**
- Deploy backend on Heroku
- Configure CORS to allow Vercel domain

**Option 3: AWS/GCP**
- Deploy backend on EC2/Cloud Run
- Set up proper networking and CORS

### CI/CD

Vercel automatically:
- ✅ Builds on every push
- ✅ Creates preview deployments for PRs
- ✅ Deploys to production on merge to main
- ✅ Runs build checks before deployment

### Production Checklist

Before going live:
- [ ] Set NEXTAUTH_SECRET in production
- [ ] Configure custom domain (if any)
- [ ] Set proper NEXTAUTH_URL
- [ ] Test authentication flow
- [ ] Verify all pages load correctly
- [ ] Check mobile responsiveness
- [ ] Test API routes

### Useful Commands

```bash
# Install Vercel CLI (optional)
npm i -g vercel

# Deploy from CLI
cd frontend
vercel

# Deploy to production
cd frontend
vercel --prod
```

## 📝 Notes

- Current deployment uses **mock data** (no real backend)
- Backend API integration will require separate backend deployment
- Environment variables are crucial for NextAuth to work
- Preview deployments use different URLs, configure accordingly
