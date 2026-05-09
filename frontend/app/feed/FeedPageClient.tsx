'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useSession } from 'next-auth/react';
import { usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { Post } from '@/types';
import { postApi } from '@/lib/api/postApi';
import PostForm from '@/components/PostForm';
import PostFeed from '@/components/PostFeed';

interface FeedPageClientProps {
  initialPosts: Post[];
  initialTotal: number;
  initialHasMore: boolean;
  isAuthenticated: boolean;
}

export default function FeedPageClient({
  initialPosts,
  initialTotal,
  initialHasMore,
  isAuthenticated,
}: FeedPageClientProps) {
  const { data: session } = useSession();
  const t = useTranslations('feed');
  const [posts, setPosts] = useState<Post[]>(initialPosts);
  const [total, setTotal] = useState(initialTotal);
  const [hasMore, setHasMore] = useState(initialHasMore);
  const [currentPage, setCurrentPage] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastFetchTime, setLastFetchTime] = useState(Date.now());

  const accessToken = session?.accessToken as string | undefined;
  const pathname = usePathname();
  const lastRefreshRef = useRef<number>(0);

  useEffect(() => {
    setPosts(initialPosts);
    setTotal(initialTotal);
    setHasMore(initialHasMore);
  }, [initialPosts, initialTotal, initialHasMore]);

  const refreshPosts = useCallback(async () => {
    setIsRefreshing(true);
    setError(null);

    try {
      const response = await postApi.getFeed(0, 20, {
        accessToken,
        user: session?.user ? {
          id: session.user.id as string,
          email: session.user.email,
          name: session.user.name,
          roles: ['ROLE_USER'],
        } : undefined,
      });
      setPosts(response.data);
      setTotal(response.total);
      setCurrentPage(0);
      setHasMore(response.page < response.totalPages - 1);
    } catch (err) {
      console.error('Error refreshing posts:', err);
    } finally {
      setIsRefreshing(false);
    }
  }, [accessToken]);

  useEffect(() => {
    refreshPosts();

    const handlePopState = () => {
      refreshPosts();
    };

    window.addEventListener('popstate', handlePopState);

    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, [refreshPosts]);

  const handlePostCreated = async () => {
    await refreshPosts();
  };

  const handleLoadMore = async () => {
    if (!hasMore) return;

    try {
      const nextPage = currentPage + 1;
      const response = await postApi.getFeed(nextPage, 20, {
        accessToken,
        user: session?.user ? {
          id: session.user.id as string,
          email: session.user.email,
          name: session.user.name,
          roles: ['ROLE_USER'],
        } : undefined,
      });

      setPosts(prev => [...prev, ...response.data]);
      setCurrentPage(nextPage);
      setHasMore(nextPage < response.totalPages - 1);
    } catch (err) {
      console.error('Error loading more posts:', err);
      setError(t('loadMoreError'));
    }
  };

  const handleLikeToggle = async (postId: string) => {
    if (!isAuthenticated || !accessToken) return;

    const postIndex = posts.findIndex(p => p.id === postId);
    if (postIndex === -1) return;

    const post = posts[postIndex];
    const previousPosts = [...posts];

    const updatedPosts = [...posts];
    updatedPosts[postIndex] = {
      ...post,
      isLikedByCurrentUser: !post.isLikedByCurrentUser,
      likeCount: post.isLikedByCurrentUser ? post.likeCount - 1 : post.likeCount + 1,
    };
    setPosts(updatedPosts);

    try {
      const response = await postApi.toggleLike(postId, {
        accessToken,
        user: session?.user ? {
          id: session.user.id as string,
          email: session.user.email,
          name: session.user.name,
          roles: ['ROLE_USER'],
        } : undefined,
      });

      const finalPosts = [...posts];
      finalPosts[postIndex] = {
        ...finalPosts[postIndex],
        isLikedByCurrentUser: response.liked,
        likeCount: response.likeCount,
      };
      setPosts(finalPosts);
    } catch (err) {
      setPosts(previousPosts);
      console.error('Error toggling like:', err);
      alert(t('likeError'));
    }
  };

  return (
    <div className="space-y-6">
      {isAuthenticated ? (
        <PostForm onPostCreated={handlePostCreated} />
      ) : (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
          <p className="text-blue-800 font-medium mb-2">
            {t('loginPrompt.title')}
          </p>
          <p className="text-blue-600 text-sm">
            {t('loginPrompt.description')}
          </p>
          <a
            href="/login"
            className="inline-block mt-4 px-6 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors duration-200"
          >
            {t('loginPrompt.button')}
          </a>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => setError(null)}
            className="mt-2 text-red-600 underline text-sm"
          >
            {t('closeError')}
          </button>
        </div>
      )}

      {isRefreshing ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          <p className="mt-4 text-gray-600">{t('refreshing')}</p>
        </div>
      ) : (
        <PostFeed
          posts={posts}
          hasMore={hasMore}
          onLoadMore={handleLoadMore}
          onLikeToggle={handleLikeToggle}
          isAuthenticated={isAuthenticated}
        />
      )}
    </div>
  );
}
