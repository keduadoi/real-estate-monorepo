import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { getPostById, addLike, removeLike } from '@/lib/mockData';

export async function POST(
  request: NextRequest,
  { params }: { params: { postId: string } }
) {
  try {
    // Check authentication (required for liking posts)
    const session = await getServerSession(authOptions);

    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    const { postId } = params;

    // Verify post exists
    const post = getPostById(postId);
    if (!post) {
      return NextResponse.json(
        { error: 'Post not found' },
        { status: 404 }
      );
    }

    // Add like
    try {
      const newLike = addLike(postId, session.user.id);

      return NextResponse.json(
        {
          success: true,
          like: newLike,
        },
        { status: 201 }
      );
    } catch (error) {
      // User already liked this post
      if (error instanceof Error && error.message === 'User already liked this post') {
        return NextResponse.json(
          { error: 'Already liked' },
          { status: 409 }
        );
      }
      throw error;
    }

  } catch (error) {
    console.error('Error liking post:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { postId: string } }
) {
  try {
    // Check authentication (required for unliking posts)
    const session = await getServerSession(authOptions);

    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    const { postId } = params;

    // Verify post exists
    const post = getPostById(postId);
    if (!post) {
      return NextResponse.json(
        { error: 'Post not found' },
        { status: 404 }
      );
    }

    // Remove like
    removeLike(postId, session.user.id);

    return NextResponse.json(
      {
        success: true,
        message: 'Like removed',
      },
      { status: 200 }
    );

  } catch (error) {
    console.error('Error unliking post:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
