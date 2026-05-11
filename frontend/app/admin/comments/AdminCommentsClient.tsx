'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { CommentDTO } from '@/types/api';
import { commentApi } from '@/lib/api/commentApi';

interface Props {
  userId: string;
  userEmail?: string | null;
  userName?: string | null;
  userRoles: string[];
  accessToken?: string;
}

export default function AdminCommentsClient(props: Props) {
  const t = useTranslations();
  const [data, setData] = useState<CommentDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const opts = {
    accessToken: props.accessToken,
    user: {
      id: props.userId,
      email: props.userEmail,
      name: props.userName,
      roles: props.userRoles,
    },
  };

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await commentApi.listReported(0, 50, opts);
      setData(res.data);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.userId]);

  useEffect(() => {
    load();
  }, [load]);

  const hide = async (id: number) => {
    const reason = prompt(t('admin.comments.hidePrompt'), 'spam');
    if (!reason) return;
    try {
      await commentApi.hide(id, reason, opts);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : String(e));
    }
  };

  const restore = async (id: number) => {
    if (!confirm(t('admin.comments.restoreConfirm'))) return;
    try {
      await commentApi.restore(id, opts);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : String(e));
    }
  };

  if (loading) return <div className="text-sm text-gray-500">{t('common.loading')}</div>;
  if (error) return <div className="text-sm text-red-600">{error}</div>;
  if (data.length === 0) return <div className="text-sm text-gray-500">{t('admin.comments.empty')}</div>;

  return (
    <div className="space-y-3">
      {data.map((c) => (
        <div
          key={c.id}
          className={`bg-white border rounded-lg p-4 ${c.hidden ? 'border-red-200 bg-red-50/40' : 'border-gray-200'}`}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="text-sm">
                <span className="font-semibold">{c.displayName || 'Anonymous'}</span>
                {' · '}
                <Link
                  href={`/properties/${c.propertyId}`}
                  className="text-primary-600 hover:underline"
                >
                  Property #{c.propertyId}
                </Link>
                {' · '}
                <span className="text-xs text-gray-500">
                  {new Date(c.createdAt).toLocaleString()}
                </span>
                {c.hidden && (
                  <span className="ml-2 px-1.5 py-0.5 rounded bg-red-100 text-red-700 text-xs">
                    {t('admin.comments.hiddenBadge')}
                  </span>
                )}
              </div>
              <p className="mt-2 text-sm text-gray-800 whitespace-pre-wrap break-words">
                {c.body || <em className="text-gray-400">{t('comments.hidden')}</em>}
              </p>
            </div>
            <div className="flex flex-col gap-2 shrink-0">
              {!c.hidden ? (
                <button
                  type="button"
                  onClick={() => hide(c.id)}
                  className="px-3 py-1 bg-red-600 text-white text-xs rounded hover:bg-red-700"
                >
                  {t('admin.comments.hide')}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => restore(c.id)}
                  className="px-3 py-1 bg-gray-200 text-gray-800 text-xs rounded hover:bg-gray-300"
                >
                  {t('admin.comments.restore')}
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
