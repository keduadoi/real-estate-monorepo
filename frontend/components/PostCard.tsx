'use client';

import { useLocale, useTranslations } from 'next-intl';
import { Post } from '@/types';
import { toIntlLocale } from '@/lib/i18n/intlLocale';
import LikeButton from './LikeButton';

interface PostCardProps {
  post: Post;
  onLikeToggle?: (postId: string) => Promise<void>;
  isAuthenticated: boolean;
}

export default function PostCard({ post, onLikeToggle, isAuthenticated }: PostCardProps) {
  const t = useTranslations('components.postCard');
  const intlLocale = toIntlLocale(useLocale());

  function formatTimestamp(timestamp: string): string {
    const now = new Date();
    const normalizedTimestamp = timestamp && !timestamp.endsWith('Z') && !timestamp.includes('+')
      ? timestamp + 'Z'
      : timestamp;
    const postDate = new Date(normalizedTimestamp);
    const diffInMs = now.getTime() - postDate.getTime();
    const diffInMinutes = Math.floor(diffInMs / 60000);
    const diffInHours = Math.floor(diffInMs / 3600000);
    const diffInDays = Math.floor(diffInMs / 86400000);

    if (diffInMinutes < 1) return t('justNow');
    if (diffInMinutes < 60) return t('minutesAgo', { count: diffInMinutes });
    if (diffInHours < 24) return t('hoursAgo', { count: diffInHours });
    if (diffInDays < 7) return t('daysAgo', { count: diffInDays });

    return postDate.toLocaleDateString(intlLocale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  const handleLikeToggle = async () => {
    if (onLikeToggle) {
      await onLikeToggle(post.id);
    }
  };

  const authorName = post.author.name || post.author.email || t('anonymous');

  const getInitials = (name: string): string => {
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-xl transition-shadow duration-300">
      <div className="flex items-start gap-4">
        <div className="flex-shrink-0">
          <div className="w-12 h-12 rounded-full bg-primary-500 text-white flex items-center justify-center font-semibold text-lg">
            {getInitials(authorName)}
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <h3 className="text-base font-semibold text-gray-900 truncate">
            {authorName}
          </h3>
          <p className="text-sm text-gray-500">
            {formatTimestamp(post.createdAt)}
          </p>
        </div>
      </div>

      <div className="mt-4">
        <p className="text-gray-900 whitespace-pre-wrap break-words">
          {post.content}
        </p>
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200">
        <LikeButton
          postId={post.id}
          isLiked={post.isLikedByCurrentUser}
          likeCount={post.likeCount}
          onToggle={handleLikeToggle}
          disabled={!isAuthenticated}
        />
      </div>
    </div>
  );
}
