'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import { CommentDTO, CommentListResponse } from '@/types/api';
import { commentApi } from '@/lib/api/commentApi';
import CommentComposer from './CommentComposer';
import CommentItem from './CommentItem';

const COMMENTS_ENABLED = process.env.NEXT_PUBLIC_COMMENTS === '1';
const PAGE_SIZE = 10;

interface Props {
  propertyId: number;
}

export default function PropertyComments({ propertyId }: Props) {
  const t = useTranslations();
  const { data: session } = useSession();
  const [data, setData] = useState<CommentDTO[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const userOpts = session?.user
    ? {
        accessToken: (session as { accessToken?: string }).accessToken,
        user: {
          id: session.user.id,
          email: session.user.email,
          name: session.user.name,
          roles: session.user.roles,
        },
      }
    : undefined;

  const load = useCallback(
    async (nextPage: number, append: boolean) => {
      setLoading(true);
      setError(null);
      try {
        const res: CommentListResponse = await commentApi.list(
          propertyId,
          nextPage,
          PAGE_SIZE,
          userOpts,
        );
        setData((prev) => (append ? [...prev, ...res.data] : res.data));
        setTotal(res.total);
        setTotalPages(res.totalPages);
        setPage(res.page);
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [propertyId, session?.user?.id],
  );

  useEffect(() => {
    if (!COMMENTS_ENABLED) {
      setLoading(false);
      return;
    }
    load(0, false);
  }, [load]);

  if (!COMMENTS_ENABLED) return null;

  return (
    <section className="mt-10 border-t border-gray-200 pt-8">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">
        {t('comments.heading', { count: total })}
      </h2>

      <div className="bg-white border border-gray-200 rounded-lg p-4 mb-4">
        <CommentComposer
          propertyId={propertyId}
          onSubmitted={() => load(0, false)}
        />
      </div>

      {error && (
        <div className="mb-4 text-sm text-red-600">{error}</div>
      )}

      {!loading && data.length === 0 && !error && (
        <div className="text-sm text-gray-500 py-6 text-center">
          {t('comments.empty')}
        </div>
      )}

      <div className="divide-y divide-gray-100">
        {data.map((c) => (
          <CommentItem
            key={c.id}
            comment={c}
            propertyId={propertyId}
            onChanged={() => load(0, false)}
          />
        ))}
      </div>

      {page + 1 < totalPages && (
        <div className="text-center mt-4">
          <button
            type="button"
            onClick={() => load(page + 1, true)}
            disabled={loading}
            className="px-4 py-2 text-sm text-primary-600 hover:bg-primary-50 rounded disabled:opacity-50"
          >
            {loading ? t('common.loading') : t('comments.loadMore')}
          </button>
        </div>
      )}
    </section>
  );
}
