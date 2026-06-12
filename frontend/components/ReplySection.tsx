'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useLocale, useTranslations } from 'next-intl';
import { postApi, Reply } from '@/lib/api/postApi';
import { toIntlLocale } from '@/lib/i18n/intlLocale';

interface ReplySectionProps {
  postId: string;
  isAuthenticated: boolean;
  onReplyAdded: () => void;
  onReplyDeleted: () => void;
}

const PAGE_SIZE = 20;
const MAX_REPLY_CHARACTERS = 2000;

export default function ReplySection({
  postId,
  isAuthenticated,
  onReplyAdded,
  onReplyDeleted,
}: ReplySectionProps) {
  const { data: session } = useSession();
  const t = useTranslations('components.replySection');
  const tTime = useTranslations('components.postCard');
  const intlLocale = toIntlLocale(useLocale());

  const [replies, setReplies] = useState<Reply[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(0);
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadReplies = useCallback(
    async (pageToLoad: number) => {
      try {
        const response = await postApi.getReplies(postId, pageToLoad, PAGE_SIZE);
        setReplies((prev) =>
          pageToLoad === 0 ? response.data : [...prev, ...response.data]
        );
        setPage(pageToLoad);
        setHasMore(pageToLoad + 1 < response.totalPages);
      } catch {
        setError(t('generalError'));
      } finally {
        setIsLoading(false);
      }
    },
    [postId, t]
  );

  useEffect(() => {
    loadReplies(0);
  }, [loadReplies]);

  function formatTimestamp(timestamp: string): string {
    if (!timestamp) return tTime('justNow');
    const now = new Date();
    const normalizedTimestamp = timestamp && !timestamp.endsWith('Z') && !timestamp.includes('+')
      ? timestamp + 'Z'
      : timestamp;
    const replyDate = new Date(normalizedTimestamp);
    const diffInMs = now.getTime() - replyDate.getTime();
    const diffInMinutes = Math.floor(diffInMs / 60000);
    const diffInHours = Math.floor(diffInMs / 3600000);
    const diffInDays = Math.floor(diffInMs / 86400000);

    if (diffInMinutes < 1) return tTime('justNow');
    if (diffInMinutes < 60) return tTime('minutesAgo', { count: diffInMinutes });
    if (diffInHours < 24) return tTime('hoursAgo', { count: diffInHours });
    if (diffInDays < 7) return tTime('daysAgo', { count: diffInDays });

    return replyDate.toLocaleDateString(intlLocale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  const getInitials = (name: string): string => {
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const requestOptions = () => ({
    accessToken: session?.accessToken as string,
    user: session?.user ? {
      id: session.user.id as string,
      email: session.user.email,
      name: session.user.name,
      roles: ['ROLE_USER']
    } : undefined
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmed = content.trim();
    if (!trimmed || trimmed.length > MAX_REPLY_CHARACTERS) return;

    const accessToken = session?.accessToken as string | undefined;
    if (!accessToken) {
      setError(t('loginRequired'));
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const reply = await postApi.createReply(postId, { content: trimmed }, requestOptions());
      setReplies((prev) => [...prev, reply]);
      setContent('');
      onReplyAdded();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('generalError'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (replyId: string) => {
    setError(null);
    try {
      await postApi.deleteReply(postId, replyId, requestOptions());
      setReplies((prev) => prev.filter((reply) => reply.id !== replyId));
      onReplyDeleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('generalError'));
    }
  };

  const currentUserId = session?.user?.id as string | undefined;

  return (
    <div className="mt-4 pt-4 border-t border-gray-200 space-y-4">
      {isLoading ? (
        <p className="text-sm text-gray-500">{t('loading')}</p>
      ) : replies.length === 0 ? (
        <p className="text-sm text-gray-500">{t('empty')}</p>
      ) : (
        <div className="space-y-3">
          {replies.map((reply) => {
            const replyAuthorName = reply.author.name || reply.author.email || tTime('anonymous');
            return (
              <div key={reply.id} className="flex items-start gap-3 group">
                <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-400 text-white flex items-center justify-center font-semibold text-xs">
                  {getInitials(replyAuthorName)}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="bg-gray-100 rounded-2xl px-4 py-2">
                    <span className="text-sm font-semibold text-gray-900">
                      {replyAuthorName}
                    </span>
                    <p className="text-sm text-gray-900 whitespace-pre-wrap break-words">
                      {reply.content}
                    </p>
                  </div>
                  <span className="text-xs text-gray-500 ml-4">
                    {formatTimestamp(reply.createdAt)}
                  </span>
                </div>

                {currentUserId === reply.author.id && (
                  <button
                    onClick={() => handleDelete(reply.id)}
                    className="text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity p-1"
                    aria-label={t('deleteReply')}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                )}
              </div>
            );
          })}

          {hasMore && (
            <button
              onClick={() => loadReplies(page + 1)}
              className="text-sm font-medium text-primary-600 hover:text-primary-700"
            >
              {t('loadMore')}
            </button>
          )}
        </div>
      )}

      {error && (
        <div className="bg-red-50 text-red-800 p-3 rounded-md text-sm">
          {error}
        </div>
      )}

      {isAuthenticated ? (
        <form onSubmit={handleSubmit} className="flex items-start gap-2">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={t('placeholder')}
            rows={1}
            maxLength={MAX_REPLY_CHARACTERS}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-2xl text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
            disabled={isSubmitting}
          />
          <button
            type="submit"
            disabled={!content.trim() || isSubmitting}
            className="px-4 py-2 bg-primary-600 text-white rounded-2xl text-sm font-medium hover:bg-primary-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors duration-200"
          >
            {isSubmitting ? t('submitting') : t('submit')}
          </button>
        </form>
      ) : (
        <p className="text-sm text-gray-500">{t('loginRequired')}</p>
      )}
    </div>
  );
}
