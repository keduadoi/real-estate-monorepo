'use client';

import { useState } from 'react';
import Image from 'next/image';
import { useLocale, useTranslations } from 'next-intl';
import { Post } from '@/types';
import { toIntlLocale } from '@/lib/i18n/intlLocale';
import { fixImageUrl } from '@/lib/utils';
import LikeButton from './LikeButton';
import ImageLightbox from './ImageLightbox';

interface PostCardProps {
  post: Post;
  onLikeToggle?: (postId: string) => Promise<void>;
  isAuthenticated: boolean;
}

export default function PostCard({ post, onLikeToggle, isAuthenticated }: PostCardProps) {
  const t = useTranslations('components.postCard');
  const intlLocale = toIntlLocale(useLocale());
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

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

  // Defensive default: older cached responses may not include imageUrls
  const images = post.imageUrls ?? [];
  const MAX_VISIBLE_IMAGES = 4;
  const visibleImages = images.slice(0, MAX_VISIBLE_IMAGES);
  const hiddenCount = images.length - visibleImages.length;

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

      {post.content && (
        <div className="mt-4">
          <p className="text-gray-900 whitespace-pre-wrap break-words">
            {post.content}
          </p>
        </div>
      )}

      {images.length > 0 && (
        <div className={`mt-4 grid gap-1 rounded-lg overflow-hidden ${images.length === 1 ? 'grid-cols-1' : 'grid-cols-2'}`}>
          {visibleImages.map((image, index) => {
            const isLastVisible = index === visibleImages.length - 1;
            const showOverlay = isLastVisible && hiddenCount > 0;
            return (
              <button
                key={index}
                onClick={() => setLightboxIndex(index)}
                className={`relative cursor-zoom-in bg-gray-200 ${
                  images.length === 1
                    ? 'h-80 md:h-96'
                    : images.length === 3 && index === 0
                    ? 'row-span-2 h-full min-h-[20rem]'
                    : 'h-40 md:h-48'
                }`}
                aria-label={t('imageAlt', { index: index + 1, author: authorName })}
              >
                <Image
                  src={fixImageUrl(image)}
                  alt={t('imageAlt', { index: index + 1, author: authorName })}
                  fill
                  className="object-cover"
                  sizes={images.length === 1 ? '(max-width: 768px) 100vw, 60vw' : '(max-width: 768px) 50vw, 30vw'}
                />
                {showOverlay && (
                  <span className="absolute inset-0 bg-black/60 flex items-center justify-center text-white text-2xl font-semibold">
                    {t('moreImages', { count: hiddenCount })}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <div className="mt-4 pt-4 border-t border-gray-200">
        <LikeButton
          postId={post.id}
          isLiked={post.isLikedByCurrentUser}
          likeCount={post.likeCount}
          onToggle={handleLikeToggle}
          disabled={!isAuthenticated}
        />
      </div>

      {lightboxIndex !== null && (
        <ImageLightbox
          images={images}
          initialIndex={lightboxIndex}
          title={authorName}
          onClose={() => setLightboxIndex(null)}
        />
      )}
    </div>
  );
}
