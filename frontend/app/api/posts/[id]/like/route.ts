import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';

// Use Kong Gateway for post-service access
const POST_SERVICE_URL = process.env.POST_SERVICE_URL || 'http://localhost:8000';

// In-memory store for likes (shared with other routes via global)
declare global {
  var mockLikes: Map<string, Set<string>>;
}

if (!global.mockLikes) {
  global.mockLikes = new Map();
}

async function tryBackendRequest(
  method: string,
  path: string,
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
    });

    return response;
  } catch (error) {
    console.log('Backend not available, using mock data');
    return null;
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { id } = await params;
  const accessToken = session.accessToken;
  const userId = session.user.id;

  // Try backend first
  const backendResponse = await tryBackendRequest('POST', `/api/posts/${id}/like`, accessToken);

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data);
  }

  if (backendResponse && !backendResponse.ok) {
    const error = await backendResponse.json().catch(() => ({ message: 'Failed to toggle like' }));
    return NextResponse.json({ error: error.message }, { status: backendResponse.status });
  }

  // Fallback to mock data
  if (!global.mockLikes.has(id)) {
    global.mockLikes.set(id, new Set());
  }

  const postLikes = global.mockLikes.get(id)!;
  const wasLiked = postLikes.has(userId);

  if (wasLiked) {
    postLikes.delete(userId);
  } else {
    postLikes.add(userId);
  }

  return NextResponse.json({
    liked: !wasLiked,
    likeCount: postLikes.size,
  });
}
