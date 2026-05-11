'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import { CommentDTO, ReportReason } from '@/types/api';
import { commentApi } from '@/lib/api/commentApi';
import CommentComposer from './CommentComposer';

interface Props {
  comment: CommentDTO;
  propertyId: number;
  onChanged: () => void;
  /** When true, the reply button is disabled (used to flatten depth ≥ 1). */
  isReply?: boolean;
}

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diff / 60_000);
  if (min < 1) return 'just now';
  if (min < 60) return `${min}m`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h}h`;
  const d = Math.floor(h / 24);
  return `${d}d`;
}

function initial(name?: string | null): string {
  if (!name) return '?';
  return name.trim().charAt(0).toUpperCase();
}

export default function CommentItem({ comment, propertyId, onChanged, isReply = false }: Props) {
  const t = useTranslations();
  const { data: session } = useSession();
  const [showReply, setShowReply] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editBody, setEditBody] = useState(comment.body || '');
  const [busy, setBusy] = useState(false);
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

  const handleLike = async () => {
    setBusy(true);
    try {
      await commentApi.like(comment.id, userOpts);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm(t('comments.deleteConfirm'))) return;
    setBusy(true);
    try {
      await commentApi.remove(comment.id, userOpts);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!editBody.trim()) return;
    setBusy(true);
    try {
      await commentApi.update(comment.id, editBody.trim(), userOpts);
      setEditing(false);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const handleReport = async () => {
    const reason = prompt(t('comments.reportReasonPrompt'), 'spam') as ReportReason | null;
    if (!reason) return;
    if (!['spam', 'abuse', 'off-topic', 'other'].includes(reason)) {
      alert(t('comments.reportInvalidReason'));
      return;
    }
    setBusy(true);
    try {
      await commentApi.report(comment.id, { reason }, userOpts);
      alert(t('comments.reportSubmitted'));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  // Hidden stub
  if (comment.hidden) {
    return (
      <div className={`text-sm text-gray-400 italic py-2 ${isReply ? 'pl-8 border-l-2 border-gray-100' : ''}`}>
        {t('comments.hidden')}
      </div>
    );
  }

  const isOwnComment = session?.user?.id === comment.userId;

  return (
    <div className={`py-3 ${isReply ? 'pl-8 border-l-2 border-gray-100' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-semibold shrink-0">
          {initial(comment.displayName)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-baseline gap-2 text-sm">
            <span className="font-semibold text-gray-900">{comment.displayName || 'Anonymous'}</span>
            {comment.isOwnerOfProperty && (
              <span className="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 text-xs font-medium">
                {t('comments.ownerBadge')}
              </span>
            )}
            {!comment.userId && (
              <span className="px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 text-xs font-medium">
                {t('comments.guestBadge')}
              </span>
            )}
            <span className="text-gray-400 text-xs">· {relativeTime(comment.createdAt)}</span>
            {comment.updatedAt && (
              <span className="text-gray-400 text-xs">· {t('comments.editedSuffix')}</span>
            )}
          </div>

          {editing ? (
            <div className="mt-2">
              <textarea
                value={editBody}
                onChange={(e) => setEditBody(e.target.value)}
                rows={3}
                maxLength={1000}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                disabled={busy}
              />
              <div className="flex gap-2 mt-2">
                <button
                  type="button"
                  onClick={handleSaveEdit}
                  disabled={busy || !editBody.trim()}
                  className="px-3 py-1 bg-primary-600 text-white rounded text-sm hover:bg-primary-700 disabled:opacity-50"
                >
                  {t('common.save')}
                </button>
                <button
                  type="button"
                  onClick={() => { setEditing(false); setEditBody(comment.body || ''); }}
                  className="px-3 py-1 text-gray-600 rounded text-sm hover:bg-gray-100"
                >
                  {t('common.cancel')}
                </button>
              </div>
            </div>
          ) : (
            <p className="mt-1 text-sm text-gray-800 whitespace-pre-wrap break-words">
              {comment.body}
            </p>
          )}

          {!editing && (
            <div className="flex flex-wrap items-center gap-3 mt-2 text-xs">
              <button
                type="button"
                onClick={handleLike}
                disabled={busy}
                className={`flex items-center gap-1 ${
                  comment.likedByCurrent ? 'text-primary-600' : 'text-gray-500 hover:text-primary-600'
                }`}
              >
                👍 {comment.likeCount > 0 ? comment.likeCount : t('comments.like')}
              </button>
              {!isReply && (
                <button
                  type="button"
                  onClick={() => setShowReply((v) => !v)}
                  className="text-gray-500 hover:text-primary-600"
                >
                  ↩ {t('comments.reply')}
                </button>
              )}
              {comment.editable && isOwnComment && (
                <button
                  type="button"
                  onClick={() => setEditing(true)}
                  className="text-gray-500 hover:text-primary-600"
                >
                  ✎ {t('comments.edit')}
                </button>
              )}
              {(isOwnComment || session?.user?.roles?.includes('ROLE_ADMIN')) && (
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={busy}
                  className="text-gray-500 hover:text-red-600"
                >
                  🗑 {t('comments.delete')}
                </button>
              )}
              <button
                type="button"
                onClick={handleReport}
                disabled={busy}
                className="text-gray-400 hover:text-red-600"
              >
                ⚑ {t('comments.report')}
              </button>
            </div>
          )}

          {error && <div className="mt-2 text-xs text-red-600">{error}</div>}

          {showReply && !isReply && (
            <div className="mt-3">
              <CommentComposer
                propertyId={propertyId}
                parentId={comment.id}
                onSubmitted={() => { setShowReply(false); onChanged(); }}
                onCancel={() => setShowReply(false)}
                compact
              />
            </div>
          )}

          {comment.replies && comment.replies.length > 0 && (
            <div className="mt-3 space-y-1">
              {comment.replies.map((r) => (
                <CommentItem
                  key={r.id}
                  comment={r}
                  propertyId={propertyId}
                  onChanged={onChanged}
                  isReply
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
