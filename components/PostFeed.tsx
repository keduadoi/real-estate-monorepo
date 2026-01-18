'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { PostWithMetadata } from '@/types';
import PostCard from './PostCard';

interface PostFeedProps {
  initialPosts: PostWithMetadata[];
  isAuthenticated: boolean;
}

const POSTS_PER_PAGE = 10;

export default function PostFeed({ initialPosts, isAuthenticated }: PostFeedProps) {
  const [allPosts, setAllPosts] = useState<PostWithMetadata[]>(initialPosts);
  const [displayedPosts, setDisplayedPosts] = useState<PostWithMetadata[]>(
    initialPosts.slice(0, POSTS_PER_PAGE)
  );
  const [page, setPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(initialPosts.length > POSTS_PER_PAGE);

  const observerTarget = useRef<HTMLDivElement>(null);

  // Update all posts when initialPosts change (e.g., after creating a new post)
  useEffect(() => {
    setAllPosts(initialPosts);
    setDisplayedPosts(initialPosts.slice(0, POSTS_PER_PAGE));
    setPage(1);
    setHasMore(initialPosts.length > POSTS_PER_PAGE);
  }, [initialPosts]);

  // Load more posts
  const loadMorePosts = useCallback(() => {
    if (isLoading || !hasMore) return;

    setIsLoading(true);

    // Simulate loading delay (like fetching from API)
    setTimeout(() => {
      const nextPage = page + 1;
      const startIndex = page * POSTS_PER_PAGE;
      const endIndex = startIndex + POSTS_PER_PAGE;
      const newPosts = allPosts.slice(startIndex, endIndex);

      if (newPosts.length > 0) {
        setDisplayedPosts(prev => [...prev, ...newPosts]);
        setPage(nextPage);
        setHasMore(endIndex < allPosts.length);
      } else {
        setHasMore(false);
      }

      setIsLoading(false);
    }, 500);
  }, [allPosts, page, isLoading, hasMore]);

  // Intersection Observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoading) {
          loadMorePosts();
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
  }, [loadMorePosts, hasMore, isLoading]);

  const handleLikeToggle = async (postId: string) => {
    if (!isAuthenticated) return;

    // Find the post in both arrays
    const displayedIndex = displayedPosts.findIndex(p => p.id === postId);
    const allIndex = allPosts.findIndex(p => p.id === postId);

    if (displayedIndex === -1 || allIndex === -1) return;

    const post = displayedPosts[displayedIndex];
    const isLiked = post.isLikedByCurrentUser;

    // Store previous state for reverting
    const previousDisplayedPosts = [...displayedPosts];
    const previousAllPosts = [...allPosts];

    // Optimistic update for both arrays
    const updatedDisplayedPosts = [...displayedPosts];
    updatedDisplayedPosts[displayedIndex] = {
      ...post,
      isLikedByCurrentUser: !isLiked,
      likeCount: isLiked ? post.likeCount - 1 : post.likeCount + 1,
    };

    const updatedAllPosts = [...allPosts];
    updatedAllPosts[allIndex] = {
      ...updatedAllPosts[allIndex],
      isLikedByCurrentUser: !isLiked,
      likeCount: isLiked ? allPosts[allIndex].likeCount - 1 : allPosts[allIndex].likeCount + 1,
    };

    setDisplayedPosts(updatedDisplayedPosts);
    setAllPosts(updatedAllPosts);

    try {
      // Make API request
      const response = await fetch(`/api/posts/${postId}/like`, {
        method: isLiked ? 'DELETE' : 'POST',
      });

      const data = await response.json();

      if (!response.ok) {
        // Revert on error
        setDisplayedPosts(previousDisplayedPosts);
        setAllPosts(previousAllPosts);

        console.error('Error toggling like:', {
          status: response.status,
          error: data.error,
          action: isLiked ? 'unlike' : 'like',
          postId,
        });

        // Show error to user
        alert(`Lỗi: ${data.error || 'Không thể thực hiện thao tác'}`);
      } else {
        console.log('Successfully toggled like:', {
          action: isLiked ? 'unlike' : 'like',
          postId,
          response: data,
        });
      }
    } catch (error) {
      // Revert on error
      setDisplayedPosts(previousDisplayedPosts);
      setAllPosts(previousAllPosts);
      console.error('Error toggling like:', error);
      alert('Đã có lỗi xảy ra. Vui lòng thử lại.');
    }
  };

  // Empty state
  if (displayedPosts.length === 0 && !isLoading) {
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
      {displayedPosts.map((post) => (
        <PostCard
          key={post.id}
          post={post}
          onLikeToggle={handleLikeToggle}
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
      {!hasMore && displayedPosts.length > 0 && (
        <div className="text-center py-8">
          <p className="text-gray-500 text-sm">
            Bạn đã xem hết tất cả bài viết
          </p>
        </div>
      )}
    </div>
  );
}
