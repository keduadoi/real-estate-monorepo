import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';

// Use Kong Gateway for post-service access
const POST_SERVICE_URL = process.env.POST_SERVICE_URL || 'http://localhost:8000';

// In-memory store for development (shared via global)
declare global {
  var mockPosts: any[];
  var postIdCounter: number;
}

if (!global.mockPosts) {
  global.mockPosts = [];
}
if (!global.postIdCounter) {
  global.postIdCounter = 1;
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

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const session = await getServerSession(authOptions);
  const accessToken = session?.accessToken;
  const { id } = await params;

  // Try backend first
  const backendResponse = await tryBackendRequest('GET', `/api/posts/${id}`, undefined, accessToken);

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data);
  }

  if (backendResponse && !backendResponse.ok) {
    if (backendResponse.status === 404) {
      return NextResponse.json({ error: 'Post not found' }, { status: 404 });
    }
    const error = await backendResponse.json().catch(() => ({ message: 'Failed to fetch post' }));
    return NextResponse.json({ error: error.message }, { status: backendResponse.status });
  }

  // Fallback to mock data
  const post = global.mockPosts.find((p) => p.id === id);

  if (!post) {
    return NextResponse.json({ error: 'Post not found' }, { status: 404 });
  }

  const userId = session?.user?.id;
  const postLikes = global.mockLikes?.get(id) || new Set();

  return NextResponse.json({
    ...post,
    likeCount: postLikes.size,
    isLikedByCurrentUser: userId ? postLikes.has(userId) : false,
  });
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { id } = await params;
  const body = await request.json();
  const { content } = body;

  if (!content || content.trim().length === 0) {
    return NextResponse.json({ error: 'Content is required' }, { status: 400 });
  }

  const accessToken = session.accessToken;

  // Try backend first
  const backendResponse = await tryBackendRequest('PUT', `/api/posts/${id}`, { content }, accessToken);

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data);
  }

  if (backendResponse && !backendResponse.ok) {
    const error = await backendResponse.json().catch(() => ({ message: 'Failed to update post' }));
    return NextResponse.json({ error: error.message }, { status: backendResponse.status });
  }

  // Fallback to mock data
  const postIndex = global.mockPosts.findIndex((p) => p.id === id);

  if (postIndex === -1) {
    return NextResponse.json({ error: 'Post not found' }, { status: 404 });
  }

  const post = global.mockPosts[postIndex];

  if (post.author.id !== session.user.id) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  post.content = content.trim();
  post.updatedAt = new Date().toISOString();

  const postLikes = global.mockLikes?.get(id) || new Set();

  return NextResponse.json({
    ...post,
    likeCount: postLikes.size,
    isLikedByCurrentUser: postLikes.has(session.user.id),
  });
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { id } = await params;
  const accessToken = session.accessToken;

  // Try backend first
  const backendResponse = await tryBackendRequest('DELETE', `/api/posts/${id}`, undefined, accessToken);

  if (backendResponse?.ok) {
    return new NextResponse(null, { status: 204 });
  }

  if (backendResponse && !backendResponse.ok) {
    const error = await backendResponse.json().catch(() => ({ message: 'Failed to delete post' }));
    return NextResponse.json({ error: error.message }, { status: backendResponse.status });
  }

  // Fallback to mock data
  const postIndex = global.mockPosts.findIndex((p) => p.id === id);

  if (postIndex === -1) {
    return NextResponse.json({ error: 'Post not found' }, { status: 404 });
  }

  const post = global.mockPosts[postIndex];

  if (post.author.id !== session.user.id) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  global.mockPosts.splice(postIndex, 1);
  global.mockLikes?.delete(id);

  return new NextResponse(null, { status: 204 });
}
