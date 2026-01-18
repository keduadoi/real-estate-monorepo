'use client';

import { PostWithMetadata } from '@/types';
import LikeButton from './LikeButton';

interface PostCardProps {
  post: PostWithMetadata;
  onLikeToggle?: (postId: string) => void;
  isAuthenticated: boolean;
}

// Format timestamp to Vietnamese
function formatTimestamp(timestamp: string): string {
  const now = new Date();
  const postDate = new Date(timestamp);
  const diffInMs = now.getTime() - postDate.getTime();
  const diffInMinutes = Math.floor(diffInMs / 60000);
  const diffInHours = Math.floor(diffInMs / 3600000);
  const diffInDays = Math.floor(diffInMs / 86400000);

  if (diffInMinutes < 1) return 'Vừa xong';
  if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
  if (diffInHours < 24) return `${diffInHours} giờ trước`;
  if (diffInDays < 7) return `${diffInDays} ngày trước`;

  // Format as date if older than 7 days
  return postDate.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

export default function PostCard({ post, onLikeToggle, isAuthenticated }: PostCardProps) {
  const handleLikeToggle = () => {
    if (onLikeToggle) {
      onLikeToggle(post.id);
    }
  };

  // Get initials from name for avatar
  const getInitials = (name: string): string => {
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-xl transition-shadow duration-300">
      {/* Header: User Info */}
      <div className="flex items-start gap-4">
        {/* Avatar */}
        <div className="flex-shrink-0">
          <div className="w-12 h-12 rounded-full bg-primary-500 text-white flex items-center justify-center font-semibold text-lg">
            {getInitials(post.user.name)}
          </div>
        </div>

        {/* User Name & Timestamp */}
        <div className="flex-1 min-w-0">
          <h3 className="text-base font-semibold text-gray-900 truncate">
            {post.user.name}
          </h3>
          <p className="text-sm text-gray-500">
            {formatTimestamp(post.createdAt)}
          </p>
        </div>
      </div>

      {/* Content */}
      <div className="mt-4">
        <p className="text-gray-900 whitespace-pre-wrap break-words">
          {post.content}
        </p>
      </div>

      {/* Footer: Like Button */}
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
