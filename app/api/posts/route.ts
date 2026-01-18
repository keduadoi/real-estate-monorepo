import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { addPost, getPostsWithMetadata } from '@/lib/mockData';

export async function GET(request: NextRequest) {
  try {
    // Get current user session (optional - feed is public)
    const session = await getServerSession(authOptions);
    const currentUserId = session?.user?.id;

    // Get posts with metadata
    const posts = getPostsWithMetadata(currentUserId);

    return NextResponse.json(
      {
        success: true,
        posts,
      },
      { status: 200 }
    );

  } catch (error) {
    console.error('Error fetching posts:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    // Check authentication (required for creating posts)
    const session = await getServerSession(authOptions);

    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    // Parse request body
    const body = await request.json();
    const { content } = body;

    // Validate required fields
    if (!content || typeof content !== 'string') {
      return NextResponse.json(
        { error: 'Content is required' },
        { status: 400 }
      );
    }

    // Validate content length
    if (content.trim().length === 0) {
      return NextResponse.json(
        { error: 'Content cannot be empty' },
        { status: 400 }
      );
    }

    if (content.length > 500) {
      return NextResponse.json(
        { error: 'Content must be 500 characters or less' },
        { status: 400 }
      );
    }

    // Create new post
    const newPost = addPost({
      content: content.trim(),
      userId: session.user.id,
    });

    return NextResponse.json(
      {
        success: true,
        post: newPost,
      },
      { status: 201 }
    );

  } catch (error) {
    console.error('Error creating post:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
