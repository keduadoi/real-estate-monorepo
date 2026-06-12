import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';

// Use Kong Gateway for post-service access
const POST_SERVICE_URL = process.env.POST_SERVICE_URL || 'http://localhost:8000';

// In-memory store for development (shared via global)
declare global {
  var mockPosts: any[];
  var postIdCounter: number;
  var mockLikes: Map<string, Set<string>>;
}

if (!global.mockPosts) {
  global.mockPosts = [];
}
if (!global.postIdCounter) {
  global.postIdCounter = 1;
}
if (!global.mockLikes) {
  global.mockLikes = new Map();
}

async function tryBackendRequest(
  method: string,
  path: string,
  body?: any,
  accessToken?: string
): Promise<Response | null> {
  try {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${POST_SERVICE_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    return response;
  } catch (error) {
    console.log('Backend not available, using mock data');
    return null;
  }
}

export async function GET(request: NextRequest) {
  const session = await getServerSession(authOptions);
  const accessToken = session?.accessToken;

  // Try backend first
  const backendResponse = await tryBackendRequest('GET', '/api/posts', undefined, accessToken);

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data);
  }

  // Fallback to mock data
  const userId = session?.user?.id;
  const postsWithMeta = global.mockPosts.map(post => {
    const postLikes = global.mockLikes.get(post.id) || new Set();
    return {
      ...post,
      likeCount: postLikes.size,
      isLikedByCurrentUser: userId ? postLikes.has(userId) : false,
    };
  }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return NextResponse.json({
    data: postsWithMeta,
    total: postsWithMeta.length,
    page: 0,
    perPage: 20,
    totalPages: Math.ceil(postsWithMeta.length / 20),
  });
}

export async function POST(request: NextRequest) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const body = await request.json();
  const { content, imageUrls } = body;

  const hasContent = typeof content === 'string' && content.trim().length > 0;
  const hasImages = Array.isArray(imageUrls) && imageUrls.length > 0;
  if (!hasContent && !hasImages) {
    return NextResponse.json({ error: 'Content or images required' }, { status: 400 });
  }

  const accessToken = session.accessToken;

  // Try backend first
  const backendResponse = await tryBackendRequest('POST', '/api/posts', { content, imageUrls }, accessToken);

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data, { status: 201 });
  }

  if (backendResponse && !backendResponse.ok) {
    const error = await backendResponse.json().catch(() => ({ message: 'Failed to create post' }));
    return NextResponse.json({ error: error.message }, { status: backendResponse.status });
  }

  // Fallback to mock data
  const newPost = {
    id: `mock-${global.postIdCounter++}`,
    content: hasContent ? content.trim() : null,
    imageUrls: hasImages ? imageUrls : [],
    author: {
      id: session.user.id,
      name: session.user.name || null,
      email: session.user.email || null,
    },
    likeCount: 0,
    isLikedByCurrentUser: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  global.mockPosts.unshift(newPost);

  return NextResponse.json(newPost, { status: 201 });
}
