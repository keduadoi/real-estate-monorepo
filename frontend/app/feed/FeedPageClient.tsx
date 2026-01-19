'use client';

import { useState } from 'react';
import { PostWithMetadata } from '@/types';
import PostForm from '@/components/PostForm';
import PostFeed from '@/components/PostFeed';

interface FeedPageClientProps {
  initialPosts: PostWithMetadata[];
  isAuthenticated: boolean;
}

export default function FeedPageClient({
  initialPosts,
  isAuthenticated,
}: FeedPageClientProps) {
  const [posts, setPosts] = useState<PostWithMetadata[]>(initialPosts);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handlePostCreated = async () => {
    // Refresh posts after creating a new post
    setIsRefreshing(true);

    try {
      const response = await fetch('/api/posts');
      const data = await response.json();

      if (data.success) {
        setPosts(data.posts);
      }
    } catch (error) {
      console.error('Error refreshing posts:', error);
    } finally {
      setIsRefreshing(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Post Form - Only shown when authenticated */}
      {isAuthenticated ? (
        <PostForm onPostCreated={handlePostCreated} />
      ) : (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
          <p className="text-blue-800 font-medium mb-2">
            Đăng nhập để đăng bài viết
          </p>
          <p className="text-blue-600 text-sm">
            Bạn cần đăng nhập để có thể tạo bài viết và tương tác với cộng đồng
          </p>
          <a
            href="/login"
            className="inline-block mt-4 px-6 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors duration-200"
          >
            Đăng nhập
          </a>
        </div>
      )}

      {/* Post Feed */}
      {isRefreshing ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          <p className="mt-4 text-gray-600">Đang tải bài viết mới...</p>
        </div>
      ) : (
        <PostFeed initialPosts={posts} isAuthenticated={isAuthenticated} />
      )}
    </div>
  );
}
