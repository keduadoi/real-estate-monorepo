'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Post } from '@/types';
import PostCard from './PostCard';

interface PostFeedProps {
  posts: Post[];
  hasMore: boolean;
  onLoadMore: () => Promise<void>;
  onLikeToggle: (postId: string) => Promise<void>;
  isAuthenticated: boolean;
}

export default function PostFeed({
  posts,
  hasMore,
  onLoadMore,
  onLikeToggle,
  isAuthenticated,
}: PostFeedProps) {
  const [isLoading, setIsLoading] = useState(false);
  const observerTarget = useRef<HTMLDivElement>(null);

  // Load more posts with loading state
  const loadMore = useCallback(async () => {
    if (isLoading || !hasMore) return;

    setIsLoading(true);
    try {
      await onLoadMore();
    } finally {
      setIsLoading(false);
    }
  }, [isLoading, hasMore, onLoadMore]);

  // Intersection Observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoading) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [loadMore, hasMore, isLoading]);

  // Empty state
  if (posts.length === 0 && !isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-12 text-center">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="currentColor"
          className="w-12 h-12 text-gray-400 mx-auto mb-4"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z"
          />
        </svg>
        <h3 className="text-lg font-medium text-gray-900 mb-2">
          Chưa có bài viết nào
        </h3>
        <p className="text-sm text-gray-500">
          Hãy là người đầu tiên chia sẻ ý kiến của bạn!
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {posts.map((post) => (
        <PostCard
          key={post.id}
          post={post}
          onLikeToggle={onLikeToggle}
          isAuthenticated={isAuthenticated}
        />
      ))}

      {/* Loading indicator */}
      {isLoading && (
        <div className="text-center py-8">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          <p className="text-gray-600 mt-2">Đang tải thêm bài viết...</p>
        </div>
      )}

      {/* Intersection observer target */}
      {hasMore && !isLoading && (
        <div ref={observerTarget} className="h-10" />
      )}

      {/* End of feed message */}
      {!hasMore && posts.length > 0 && (
        <div className="text-center py-8">
          <p className="text-gray-500 text-sm">
            Bạn đã xem hết tất cả bài viết
          </p>
        </div>
      )}
    </div>
  );
}
