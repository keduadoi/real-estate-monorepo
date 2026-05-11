'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import { CaptchaChallenge } from '@/types/api';
import { commentApi } from '@/lib/api/commentApi';

interface Props {
  propertyId: number;
  /** When set, this composer creates a reply to the given parent comment. */
  parentId?: number;
  onSubmitted: () => void;
  onCancel?: () => void;
  /** Compact mode used inside replies — smaller textarea, no heading. */
  compact?: boolean;
}

export default function CommentComposer({
  propertyId,
  parentId,
  onSubmitted,
  onCancel,
  compact = false,
}: Props) {
  const t = useTranslations();
  const { data: session } = useSession();
  const isAuth = !!session?.user;

  const [body, setBody] = useState('');
  const [name, setName] = useState('');
  const [captcha, setCaptcha] = useState<CaptchaChallenge | null>(null);
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [website, setWebsite] = useState(''); // honeypot
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

  // Anonymous users need a captcha. Fetch lazily when the textarea is focused.
  const ensureCaptcha = async () => {
    if (isAuth || captcha) return;
    try {
      const c = await commentApi.getCaptcha();
      setCaptcha(c);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  useEffect(() => {
    if (!isAuth) ensureCaptcha();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuth]);

  const reset = () => {
    setBody('');
    setName('');
    setCaptchaAnswer('');
    setCaptcha(null);
    setWebsite('');
    setError(null);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!body.trim() || busy) return;
    setBusy(true);
    setError(null);
    try {
      const payload = {
        body: body.trim(),
        displayName: isAuth ? undefined : name.trim(),
        captchaId: isAuth ? undefined : captcha?.captchaId,
        captchaAnswer: isAuth ? undefined : captchaAnswer.trim(),
        website,
      };
      if (parentId) {
        await commentApi.reply(parentId, payload, userOpts);
      } else {
        await commentApi.create(propertyId, payload, userOpts);
      }
      reset();
      onSubmitted();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      // refresh captcha after a failed submit (it was consumed server-side)
      if (!isAuth) {
        try {
          setCaptcha(await commentApi.getCaptcha());
          setCaptchaAnswer('');
        } catch {
          /* ignore */
        }
      }
    } finally {
      setBusy(false);
    }
  };

  const charCount = body.length;
  const overLimit = charCount > 1000;

  return (
    <form onSubmit={submit} className={compact ? '' : 'mt-2'}>
      {!isAuth && (
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t('comments.composerNamePlaceholder')}
          maxLength={50}
          required
          className="w-full px-3 py-2 mb-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          disabled={busy}
        />
      )}

      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        onFocus={ensureCaptcha}
        placeholder={t('comments.composerBodyPlaceholder')}
        rows={compact ? 2 : 3}
        maxLength={1100}
        required
        className={`w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
          overLimit ? 'border-red-300' : 'border-gray-300'
        }`}
        disabled={busy}
      />

      {/* Honeypot — visible only to bots that auto-fill all inputs */}
      <input
        type="text"
        value={website}
        onChange={(e) => setWebsite(e.target.value)}
        tabIndex={-1}
        autoComplete="off"
        aria-hidden="true"
        className="hidden"
      />

      <div className="flex items-center justify-between mt-2 gap-3 flex-wrap">
        {!isAuth && captcha && (
          <div className="flex items-center gap-2 text-sm">
            <span className="text-gray-700">{captcha.question} =</span>
            <input
              type="text"
              value={captchaAnswer}
              onChange={(e) => setCaptchaAnswer(e.target.value)}
              required
              maxLength={5}
              className="w-16 px-2 py-1 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              disabled={busy}
            />
          </div>
        )}

        <div className="flex items-center gap-3 ml-auto">
          {charCount > 800 && (
            <span className={`text-xs ${overLimit ? 'text-red-600' : 'text-gray-400'}`}>
              {charCount} / 1000
            </span>
          )}
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded"
            >
              {t('comments.cancel')}
            </button>
          )}
          <button
            type="submit"
            disabled={busy || !body.trim() || overLimit || (!isAuth && !captcha)}
            className="px-4 py-1.5 bg-primary-600 text-white text-sm font-medium rounded hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {busy ? t('common.loading') : t('comments.submit')}
          </button>
        </div>
      </div>

      {error && <div className="mt-2 text-xs text-red-600">{error}</div>}
    </form>
  );
}
