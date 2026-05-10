'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { aiSearchApi } from '@/lib/api/aiSearchApi';
import { AiSearchParseResponse } from '@/types/api';

const AI_SEARCH_ENABLED = process.env.NEXT_PUBLIC_AI_SEARCH === '1';

// /search reads UI-shape enums ('villa', 'for-sale') and re-maps to API enums
// itself, so we must hand it the lowercase form — not the API form the parser returns.
const TYPE_TO_UI: Record<string, string> = {
  HOUSE: 'house',
  APARTMENT: 'apartment',
  VILLA: 'villa',
  TOWNHOUSE: 'townhouse',
};
const STATUS_TO_UI: Record<string, string> = {
  FOR_SALE: 'for-sale',
  FOR_RENT: 'for-rent',
};

// /search also expects price in millions (it multiplies by 1e6 server-side).
const toMillions = (n: number) => Math.round(n / 1_000_000);

function parseToSearchParams(
  parsed: AiSearchParseResponse,
  removed: Set<string>
): URLSearchParams {
  const params = new URLSearchParams();
  const f = parsed.filters;

  if (f.propertyType && !removed.has('propertyType')) {
    const ui = TYPE_TO_UI[f.propertyType];
    if (ui) params.set('propertyType', ui);
  }
  if (f.status && !removed.has('status')) {
    const ui = STATUS_TO_UI[f.status];
    if (ui) params.set('status', ui);
  }
  if (f.bedrooms != null && !removed.has('bedrooms')) {
    params.set('bedrooms', String(f.bedrooms));
  }
  if (f.minPrice != null && !removed.has('minPrice')) {
    params.set('minPrice', String(toMillions(f.minPrice)));
  }
  if (f.maxPrice != null && !removed.has('maxPrice')) {
    params.set('maxPrice', String(toMillions(f.maxPrice)));
  }
  if (f.cities && f.cities.length > 0 && !removed.has('cities')) {
    // Existing /search backend takes a single city; pick the first.
    params.set('city', f.cities[0]);
  }
  // Only pass freeText as keyword. originalQuery would force the backend to
  // LIKE-match the whole sentence against title/description, which almost
  // never has hits — better to rely on the structured filters we just set.
  if (f.freeText && !removed.has('freeText')) {
    params.set('query', f.freeText);
  }

  return params;
}

export default function AiSearchBar() {
  const router = useRouter();
  const t = useTranslations();
  const locale = useLocale();

  const [query, setQuery] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!AI_SEARCH_ENABLED) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!query.trim() || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const parsed = await aiSearchApi.parse(query.trim(), locale);
      const params = parseToSearchParams(parsed, new Set());
      // Stash the parsed response for the /search page to render chips.
      try {
        sessionStorage.setItem('aiSearch:lastParse', JSON.stringify(parsed));
      } catch {
        /* ignore quota errors */
      }
      router.push(`/search?${params.toString()}`);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'AI search failed';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 mb-6">
      <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-3">
        <div className="flex-1 relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('aiSearch.placeholder')}
            className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            disabled={submitting}
          />
        </div>
        <button
          type="submit"
          disabled={submitting || !query.trim()}
          className="px-6 py-3 bg-primary-600 text-white rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? t('common.loading') : t('aiSearch.submit')}
        </button>
      </form>
      {error && (
        <div className="mt-3 text-sm text-red-600">{error}</div>
      )}
    </div>
  );
}
